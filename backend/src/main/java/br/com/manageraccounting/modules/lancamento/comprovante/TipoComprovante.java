package br.com.manageraccounting.modules.lancamento.comprovante;

import java.util.Optional;

/**
 * Formatos aceitos para o comprovante. O tipo é descoberto pelos primeiros bytes do arquivo (assinatura), não pelo
 * nome nem pelo Content-Type que o navegador mandou, que podem ser forjados.
 */
public enum TipoComprovante {
    PDF("application/pdf", "pdf"),
    JPEG("image/jpeg", "jpg"),
    PNG("image/png", "png"),
    WEBP("image/webp", "webp");

    /** Bytes necessários para reconhecer qualquer um dos formatos. */
    public static final int BYTES_ASSINATURA = 12;

    private final String contentType;
    private final String extensao;

    TipoComprovante(String contentType, String extensao) {
        this.contentType = contentType;
        this.extensao = extensao;
    }

    public String getContentType() {
        return contentType;
    }

    public String getExtensao() {
        return extensao;
    }

    /** Reconhece o formato pelo início do arquivo; vazio quando não é um dos aceitos. */
    public static Optional<TipoComprovante> detectar(byte[] inicio) {
        if (comeca(inicio, 0x25, 0x50, 0x44, 0x46, 0x2D)) { // %PDF-
            return Optional.of(PDF);
        }
        if (comeca(inicio, 0xFF, 0xD8, 0xFF)) {
            return Optional.of(JPEG);
        }
        if (comeca(inicio, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)) {
            return Optional.of(PNG);
        }
        // RIFF....WEBP
        if (comeca(inicio, 0x52, 0x49, 0x46, 0x46) && inicio.length >= 12
                && inicio[8] == 0x57 && inicio[9] == 0x45 && inicio[10] == 0x42 && inicio[11] == 0x50) {
            return Optional.of(WEBP);
        }
        return Optional.empty();
    }

    private static boolean comeca(byte[] dados, int... assinatura) {
        if (dados == null || dados.length < assinatura.length) {
            return false;
        }
        for (int i = 0; i < assinatura.length; i++) {
            if ((dados[i] & 0xFF) != assinatura[i]) {
                return false;
            }
        }
        return true;
    }
}
