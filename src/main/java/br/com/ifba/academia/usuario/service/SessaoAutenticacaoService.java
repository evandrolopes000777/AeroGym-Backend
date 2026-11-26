package br.com.ifba.academia.usuario.service;

import br.com.ifba.academia.usuario.entity.Usuario;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SessaoAutenticacaoService {
    private static final long TEMPO_SESSAO_SEGUNDOS = 30 * 60;
    private final ConcurrentMap<String, Sessao> sessoes = new ConcurrentHashMap<>();

    public String criar(Usuario usuario) {
        String token = UUID.randomUUID().toString();
        sessoes.put(token, new Sessao(usuario.getId(), usuario.getPerfil(),
                Instant.now().plusSeconds(TEMPO_SESSAO_SEGUNDOS)));
        return token;
    }

    public void exigirAdministrador(String authorization) {
        Sessao sessao = obterSessao(authorization);
        if (!"ADMIN".equalsIgnoreCase(sessao.perfil())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso restrito a administradores.");
        }
    }

    public void revogar(String authorization) {
        String token = extrairToken(authorization);
        sessoes.remove(token);
    }

    private Sessao obterSessao(String authorization) {
        String token = extrairToken(authorization);
        Sessao sessao = sessoes.get(token);
        if (sessao == null || !sessao.expiraEm().isAfter(Instant.now())) {
            sessoes.remove(token);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sessão inválida ou expirada.");
        }
        return sessao;
    }

    private String extrairToken(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Autenticação necessária.");
        }
        return authorization.substring(7).trim();
    }

    private record Sessao(Long usuarioId, String perfil, Instant expiraEm) { }
}

