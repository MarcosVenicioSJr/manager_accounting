package br.com.manageraccounting.modules.lancamento.comprovante;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.springframework.stereotype.Component;

/**
 * Comprovantes gravados em disco: {@code <diretorio>/<tenantId>/<uuid>.<extensao>}. A chave é sempre gerada aqui,
 * então nenhum nome vindo do usuário vira caminho de arquivo. Mesmo assim, toda chave lida é conferida para não
 * sair da pasta base.
 */
@Component
public class DiscoComprovanteStorage implements ComprovanteStorage {

    private final Path base;

    public DiscoComprovanteStorage(ComprovanteProperties properties) {
        this.base = properties.diretorio().toAbsolutePath().normalize();
    }

    @Override
    public String gravar(Long tenantId, TipoComprovante tipo, InputStream conteudo) throws IOException {
        String chave = tenantId + "/" + UUID.randomUUID() + "." + tipo.getExtensao();
        Path destino = resolver(chave);
        Files.createDirectories(destino.getParent());
        // Grava num temporário e move, para nunca existir um arquivo pela metade com o nome final
        Path temporario = Files.createTempFile(destino.getParent(), ".envio-", ".tmp");
        try {
            Files.copy(conteudo, temporario, StandardCopyOption.REPLACE_EXISTING);
            Files.move(temporario, destino, StandardCopyOption.ATOMIC_MOVE);
        } finally {
            Files.deleteIfExists(temporario);
        }
        return chave;
    }

    @Override
    public InputStream abrir(String chave) throws IOException {
        return Files.newInputStream(resolver(chave));
    }

    @Override
    public void apagar(String chave) throws IOException {
        Files.deleteIfExists(resolver(chave));
    }

    private Path resolver(String chave) {
        Path caminho = base.resolve(chave).normalize();
        if (!caminho.startsWith(base)) {
            throw new IllegalArgumentException("Chave de comprovante inválida");
        }
        return caminho;
    }
}
