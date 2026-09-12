# Retirada e devolução com evidências

Implementação de 12/09/2026. Novas RETIRADAS e DEVOLUÇÕES exigem assinatura
antes da conclusão. DEVOLUÇÃO admite uma foto opcional. ENTRADA continua sendo
gerada exclusivamente pela confirmação da nota fiscal.

## Fluxo da interface

1. O operador preenche funcionário, contrato, material, quantidade e observação.
2. **Continuar** abre o `SignaturePad` existente, com resumo de tipo,
   funcionário, contrato, material, quantidade e estoque projetado.
3. O botão **Confirmar retirada** ou **Confirmar devolução** só fica disponível
   depois de desenhar. Limpar a assinatura o desabilita novamente.
4. Na devolução, **Adicionar foto** abre a seleção da galeria; **Usar câmera**
   usa outro input nativo com `capture="environment"`. O navegador/dispositivo
   decide a interface disponível. A foto tem prévia e pode ser removida ou
   substituída antes do envio. Nenhuma foto é exigida.
5. A confirmação envia os dados e arquivos juntos. Durante captura/envio, o
   modal impede reenvio, alterações e cancelamento.
6. No sucesso, formulário, assinatura e foto são limpos e o comprovante abre,
   mostrando a assinatura e a foto, quando enviada. O estoque é recarregado.

Cancelar ou fechar com Escape antes do envio não faz uma chamada de criação e
mantém os dados do formulário. Erros do backend mantêm o modal, a assinatura,
a foto escolhida e os dados. Se uma foto for recusada, o usuário pode removê-la
e concluir a devolução com a assinatura. O modal usa `dialog.showModal()`,
rolagem interna e ações fora da área que rola.

## API e prevenção de bypass

O caminho continua sendo **POST /movimentacoes**, com autenticação atual de
ADMIN ou OPERADOR e resposta `MovimentacaoResponse` atual.

O novo envio usa `multipart/form-data`:

| Parte | Conteúdo | Obrigatória |
| --- | --- | --- |
| `movimentacao` | JSON, com Content-Type `application/json`, usando `MovimentacaoRequest` | Sim |
| `assinatura` | PNG ou JPEG válido e não vazio | Sim |
| `foto` | PNG ou JPEG válido, apenas em DEVOLUCAO | Não |

Exemplo da parte JSON:

```json
{
  "funcionarioId": 1,
  "contratoId": 2,
  "materialId": 3,
  "quantidade": 4,
  "tipo": "DEVOLUCAO",
  "observacao": "Material recebido sem avarias"
}
```

O `MovimentacaoController` exige a parte de assinatura no multipart. O
`MovimentacaoService` também valida sua presença e seu conteúdo, antes de
alterar estoque ou persistir uma movimentação. O método de serviço anterior
sem parâmetros de evidência foi substituído.

O POST antigo com `application/json` continua reconhecido para retornar erro
de regra de negócio: ele chama o mesmo serviço sem assinatura e recebe 409,
sem alterações no banco. Não existe uma rota alternativa de criação sem
evidência. Clientes antigos devem passar a enviar multipart. Multipart sem a
parte obrigatória recebe 400; imagem inválida ou regra de negócio violada recebe
409; limites multipart excedidos recebem 413. As permissões não foram ampliadas.

## Transação e arquivos

`MovimentacaoService.registrarMovimentacao` mantém `@Transactional`. O fluxo é:

1. Validar DTO, tipo, quantidade e arquivos, incluindo a assinatura obrigatória.
2. Validar funcionário/contrato e obter o bloqueio de estoque já existente.
3. Atualizar estoque e persistir a movimentação, ainda sem commit.
4. Registrar assinatura e foto no `EvidenciaMovimentacaoService`, usando
   propagação `MANDATORY`, que exige a transação externa.
5. Persistir o comprovante pelo serviço existente e confirmar a transação.

O registro finalizado não fica visível como operação concluída antes do commit.
Exceções nas etapas essenciais revertem movimentação, estoque, metadados das
evidências e comprovante. Uma foto enviada também faz parte dessa operação:
falha ao processá-la reverte tudo, permitindo tentar novamente sem foto.

Como o sistema de arquivos não participa da transação do banco, cada chave recebe
uma compensação registrada **antes da escrita**. Em erro imediato há tentativa
de remoção; em rollback confirmado, `afterCompletion` remove os arquivos
associados à operação, incluindo a assinatura caso uma foto posterior falhe.
Falhas de remoção são registradas no log sem ocultar a exceção original.

Se o resultado do commit for desconhecido, os arquivos são preservados e o caso
é registrado no log: removê-los poderia eliminar evidências de uma transação
que chegou a ser confirmada. Uma interrupção abrupta do processo também pode
impedir a compensação. Esses casos exigem conferência posterior entre storage
e metadados; não foi adicionado um processo de limpeza automática nesta etapa.

## Segurança, foto e imutabilidade

`ImagemEvidenciaValidator` centraliza a validação para o novo cadastro e para
o endpoint existente de assinatura histórica:

- Limite por arquivo de `app.evidencias.tamanho-maximo`, por padrão **2 MB**,
  configurável por `APP_EVIDENCIAS_TAMANHO_MAXIMO`.
- MIME permitido: `image/png` ou `image/jpeg`, correspondente ao formato lido.
- Leitura limitada e decodificação real com ImageIO; cabeçalho falso/truncado
  não basta. Imagens acima de 20 milhões de pixels são recusadas para limitar
  a memória necessária à decodificação.
- Assinatura em branco, transparente ou uniforme é recusada por ausência de
  variação visual. Isso verifica presença de desenho, sem validar identidade.

O limite total multipart passou a **5 MB**, configurável por
`APP_MULTIPART_TAMANHO_MAXIMO`, para acomodar dois arquivos de até 2 MB e o JSON.
Ao elevar o limite por arquivo, ajuste também o limite total da requisição.

A foto usa `EvidenciaStorage`, como a assinatura. No storage local, a gravação
mantém arquivo temporário e movimentação atômica quando suportada. As chaves
usam UUID, por exemplo
`movimentacoes/{id}/foto_devolucao/{uuid}.jpg`; o nome enviado pelo cliente não
é utilizado como caminho físico. O nome original é normalizado apenas para
metadados. O banco guarda tipo, hash SHA-256, tamanho, MIME, responsáveis e chave,
sem Base64 ou conteúdo binário da imagem. Os DTOs não expõem a chave nem caminho
físico. A leitura continua autenticada e verifica integridade.

A restrição existente `(movimentacao_id, tipo)` limita a uma assinatura e uma
foto por movimentação. Evidências permanecem imutáveis e não existe endpoint
de substituição de foto. A foto é escolhida no cadastro; não foi adicionado
upload posterior de foto nem suporte a múltiplas fotos.

## Históricos e migrations

**Nenhuma migration nova.** O enum, a tabela, a unicidade por tipo e as proteções
de imutabilidade já existiam nas migrations V7/V8.

Movimentações antigas sem assinatura continuam consultáveis. O comprovante
informa **Registro histórico sem assinatura**. A ação já existente de anexar
assinatura posteriormente permanece disponível para ADMIN/OPERADOR nesses
registros; não modifica estoque nem substitui assinatura existente. Nenhum
histórico recebe assinatura automática ou alteração retroativa.

CONSULTA continua apenas visualizando comprovantes e evidências. ENTRADA
continua apresentando a NF, sem exigir assinatura ou aceitar foto de devolução.

## Arquivos desta tarefa

Criados no backend (`Projeto1`):

- `src/main/java/com/Lucca/Projeto1/service/ImagemEvidenciaValidator.java`
- `src/test/java/com/Lucca/Projeto1/ImagemEvidenciaTestSupport.java`
- `src/test/java/com/Lucca/Projeto1/MovimentacaoComEvidenciasIntegrationTests.java`
- `docs/movimentacoes-com-evidencias.md`

Criados no frontend (`projeto1-frontend`):

- `src/components/ReturnPhotoPicker.jsx`
- `src/utils/movementEvidence.js`
- `test/movementEvidence.test.js`
- `e2e/movementEvidence.spec.js`

Alterados no backend:

- `src/main/java/com/Lucca/Projeto1/controller/MovimentacaoController.java`
- `src/main/java/com/Lucca/Projeto1/service/MovimentacaoService.java`
- `src/main/java/com/Lucca/Projeto1/service/EvidenciaMovimentacaoService.java`
- `src/main/java/com/Lucca/Projeto1/exception/GlobalExceptionHandler.java`
- `src/main/resources/application.properties`
- `src/test/java/com/Lucca/Projeto1/ApiIntegrationTests.java`
- `src/test/java/com/Lucca/Projeto1/ComprovanteMovimentacaoIntegrationTests.java`

Alterados no frontend:

- `src/App.jsx`
- `src/App.css`
- `src/pages/MovementFormPage.jsx`
- `src/components/SignaturePad.jsx`
- `src/components/MovementReceiptModal.jsx`
- `src/services/movimentacaoService.js`

Outras alterações já presentes ou feitas simultaneamente na área de trabalho
foram preservadas e não fazem parte desta lista.

## Testes executados

| Verificação | Resultado |
| --- | --- |
| Maven `test` | 102 testes aprovados, zero falhas/erros/ignorados |
| `npm test` | 61 testes aprovados |
| `npm run test:e2e` com Chrome | 48 testes aprovados em desktop/mobile |
| `npm run lint` | Aprovado |
| `npm run build` | Aprovado |
| `git diff --check` | Sem erros de whitespace |

Os 16 novos casos de integração cobrem assinatura PNG/JPEG, ausência de
assinatura, POST JSON antigo, devolução com/sem foto, tipo proibido, arquivo
vazio, canvas branco, truncamento, MIME incorreto, tamanho excessivo, consulta
de históricos, permissões e rollback. As falhas simuladas incluem storage que
escreve e lança erro, falha na segunda evidência, persistência de metadados,
criação do comprovante e falha tardia antes do commit. Os testes comparam
explicitamente estoque, contagens das tabelas e arquivos antes/depois.

Os testes anteriores de estoque, concorrência, permissões, notas fiscais,
comprovantes e migrações continuam passando. Seus helpers de criação de novas
movimentações foram adaptados para enviar assinatura real. O teste de upload
posterior usa um registro histórico sem assinatura.

Os testes de interface usam a aplicação real com HTTP simulado, incluindo
multipart e bytes dos arquivos. São 22 execuções novas de movimentação (11
cenários em dois tamanhos de tela) e 26 execuções existentes de cadastro de
material pela NF. Cobrem resumo, bloqueio sem assinatura, cancelamento,
confirmação, foto opcional, galeria/câmera, prévia, remoção, erro preservando
estado, falha de captura, prevenção de reenvio, limpeza no sucesso, comprovantes,
CONSULTA e evidência NF de ENTRADA. A emulação não abre câmera ou teclado de um
celular físico.

Para repetir:

```powershell
# Em Projeto1
./mvnw.cmd test

# Em projeto1-frontend
npm test
npm run lint
npm run build
$env:PLAYWRIGHT_CHANNEL = 'chrome'
npm run test:e2e
```

## Roteiro manual no celular

Use ambiente de desenvolvimento com backend e frontend atualizados. Tenha um
material com estoque, funcionário/contrato ativos e um vínculo com saldo de
retirada para testar a devolução.

1. Entre como OPERADOR. Abra **Retirada**, preencha os dados e toque em
   **Continuar**. Confira o resumo e o botão de confirmação desabilitado.
2. Cancele sem assinar. Confira os dados preservados e, no histórico/estoque,
   ausência de nova movimentação ou alteração de saldo.
3. Continue novamente, desenhe com o dedo, use **Limpar** e confira que o botão
   volta a ficar desabilitado. Assine outra vez e toque em **Confirmar retirada**.
   Confira a assinatura no comprovante e a redução correta de estoque.
4. Feche o comprovante: os campos devem estar limpos. Abra **Devolução** e
   preencha o mesmo vínculo, dentro do saldo ainda retirado. Continue, assine
   e confirme **sem foto**. Confira assinatura e aumento correto do estoque.
5. Faça outra devolução. Toque em **Adicionar foto**, escolha PNG/JPEG da galeria
   e confira a prévia. Escolha outra imagem e use **Remover foto**. A ausência
   da foto deve continuar permitindo confirmar quando houver assinatura.
6. Teste **Usar câmera** em aparelho compatível. Tire uma foto, volte à aplicação
   e confira a prévia. Assine, confirme e confira assinatura e foto no comprovante.
   Reabra-o pelo histórico para verificar carregamento autenticado das imagens.
7. Envie uma imagem maior que o limite ou simule falha do backend em ambiente
   de teste. Confira erro no modal, assinatura/foto/dados preservados e estoque
   inalterado. Remova a foto recusada ou escolha outra e tente novamente.
8. Com rede lenta, toque em confirmar e confira o bloqueio de reenvio e de
   cancelamento durante o processamento. Verifique uma única movimentação.
9. Repita cancelamento após assinar e escolher foto, antes de confirmar. Nenhuma
   operação deve existir no histórico. Confira rolagem vertical, orientação da
   tela e acesso aos botões enquanto navega pelo resumo, assinatura e foto.
10. Entre como CONSULTA: não deve haver controles de criação/assinatura. Abra
    comprovante novo, histórico sem assinatura e ENTRADA. O histórico deve
    continuar legível, e ENTRADA deve mostrar a NF.
11. Com ADMIN, confirme também o fluxo completo. Verifique que o cadastro normal
    de NF e sua confirmação continuam funcionando sem assinatura.

Esse roteiro descreve a conferência física a realizar; os testes automatizados
foram executados em navegador com emulação mobile e backend de testes H2.
