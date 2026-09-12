package com.Lucca.Projeto1;

import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;

final class ImagemEvidenciaTestSupport {
    private ImagemEvidenciaTestSupport() { }

    static byte[] imagem(String formato, boolean comTraco) throws IOException {
        BufferedImage imagem = new BufferedImage(120, 80, BufferedImage.TYPE_INT_RGB);
        var graphics = imagem.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, 120, 80);
        if (comTraco) {
            graphics.setColor(Color.BLACK);
            graphics.drawLine(10, 50, 105, 20);
            graphics.drawLine(45, 10, 70, 65);
        }
        graphics.dispose();
        var output = new ByteArrayOutputStream();
        ImageIO.write(imagem, formato, output);
        return output.toByteArray();
    }

    static MockMultipartFile assinatura() throws IOException {
        return new MockMultipartFile("assinatura", "assinatura.png", "image/png", imagem("png", true));
    }

    static MockMultipartFile dados(String json) {
        return new MockMultipartFile("movimentacao", "", MediaType.APPLICATION_JSON_VALUE,
                json.getBytes(StandardCharsets.UTF_8));
    }

    static MockMultipartHttpServletRequestBuilder movimentacaoAssinada(String json) throws IOException {
        return multipart("/movimentacoes").file(dados(json)).file(assinatura());
    }
}
