package com.Lucca.Projeto1.service;

import com.Lucca.Projeto1.exception.RegraNegocioException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Locale;

@Component
public class ImagemEvidenciaValidator {
    private static final long MAX_PIXELS = 20_000_000;
    private final int tamanhoMaximo;

    public ImagemEvidenciaValidator(
            @Value("${app.evidencias.tamanho-maximo:2MB}") String tamanhoMaximo
    ) {
        this.tamanhoMaximo = Math.toIntExact(DataSize.parse(tamanhoMaximo).toBytes());
        if (this.tamanhoMaximo <= 0 || this.tamanhoMaximo == Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Limite de evidência inválido");
        }
    }

    public ImagemValidada validar(MultipartFile arquivo, boolean assinatura) {
        String nome = assinatura ? "assinatura" : "foto";
        if (arquivo == null || arquivo.isEmpty()) {
            throw new RegraNegocioException("O arquivo da " + nome + " é obrigatório");
        }
        if (arquivo.getSize() > tamanhoMaximo) throw tamanhoExcedido(nome);
        String mime = arquivo.getContentType() == null ? ""
                : arquivo.getContentType().split(";")[0].trim().toLowerCase(Locale.ROOT);
        if (!mime.equals("image/png") && !mime.equals("image/jpeg")) {
            throw imagemInvalida(nome);
        }

        try (InputStream input = arquivo.getInputStream()) {
            byte[] conteudo = input.readNBytes(tamanhoMaximo + 1);
            if (conteudo.length > tamanhoMaximo) throw tamanhoExcedido(nome);
            try (var imageInput = new MemoryCacheImageInputStream(new ByteArrayInputStream(conteudo))) {
                Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInput);
                if (!readers.hasNext()) throw imagemInvalida(nome);
                ImageReader reader = readers.next();
                try {
                    reader.setInput(imageInput, true, true);
                    String formato = reader.getFormatName().toLowerCase(Locale.ROOT);
                    String mimeReal = switch (formato) {
                        case "png" -> "image/png";
                        case "jpeg", "jpg" -> "image/jpeg";
                        default -> throw imagemInvalida(nome);
                    };
                    if (!mime.equals(mimeReal)) throw imagemInvalida(nome);
                    int width = reader.getWidth(0);
                    int height = reader.getHeight(0);
                    if (width <= 0 || height <= 0 || (long) width * height > MAX_PIXELS) {
                        throw imagemInvalida(nome);
                    }
                    BufferedImage imagem = reader.read(0);
                    if (imagem == null) throw imagemInvalida(nome);
                    if (assinatura && !possuiTraco(imagem)) {
                        throw new RegraNegocioException("A assinatura não pode estar em branco");
                    }
                    return new ImagemValidada(conteudo, mimeReal,
                            mimeReal.equals("image/png") ? "png" : "jpg",
                            arquivo.getOriginalFilename());
                } finally {
                    reader.dispose();
                }
            }
        } catch (IOException exception) {
            throw imagemInvalida(nome);
        }
    }

    // Detects a blank/transparent or uniform canvas, without assessing identity.
    private boolean possuiTraco(BufferedImage imagem) {
        int minimo = 255;
        int maximo = 0;
        for (int y = 0; y < imagem.getHeight(); y++) {
            for (int x = 0; x < imagem.getWidth(); x++) {
                int pixel = imagem.getRGB(x, y);
                int alpha = (pixel >>> 24) & 255;
                int media = (((pixel >>> 16) & 255) + ((pixel >>> 8) & 255) + (pixel & 255)) / 3;
                int sobreBranco = (media * alpha + 255 * (255 - alpha)) / 255;
                minimo = Math.min(minimo, sobreBranco);
                maximo = Math.max(maximo, sobreBranco);
                if (maximo - minimo >= 16) return true;
            }
        }
        return false;
    }

    private RegraNegocioException tamanhoExcedido(String nome) {
        return new RegraNegocioException("O arquivo da " + nome + " excede o tamanho máximo permitido");
    }

    private RegraNegocioException imagemInvalida(String nome) {
        return new RegraNegocioException("O arquivo da " + nome + " deve ser uma imagem PNG ou JPEG válida, com MIME correspondente");
    }

    public record ImagemValidada(byte[] conteudo, String contentType, String extensao, String nomeOriginal) { }
}
