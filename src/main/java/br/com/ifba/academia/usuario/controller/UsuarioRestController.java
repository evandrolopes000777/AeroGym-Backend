package br.com.ifba.academia.usuario.controller;

import br.com.ifba.academia.pessoa.entity.Pessoa;
import br.com.ifba.academia.pessoa.service.PessoaIService;
import br.com.ifba.academia.usuario.entity.Usuario;
import br.com.ifba.academia.usuario.service.SessaoAutenticacaoService;
import br.com.ifba.academia.usuario.service.UsuarioIService;
import java.net.URI;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

// Mantém os contratos HTTP separados da entidade JPA e não serializa a senha.
@RestController
@RequestMapping("/api/usuarios")
public class UsuarioRestController {
    private final UsuarioIService usuarioService;
    private final PessoaIService pessoaService;
    private final SessaoAutenticacaoService sessaoAutenticacaoService;

    public UsuarioRestController(UsuarioIService usuarioService, PessoaIService pessoaService,
            SessaoAutenticacaoService sessaoAutenticacaoService) {
        this.usuarioService = usuarioService;
        this.pessoaService = pessoaService;
        this.sessaoAutenticacaoService = sessaoAutenticacaoService;
    }

    @GetMapping
    public List<RespostaUsuario> findAll(@RequestHeader(value = "Authorization", required = false) String autorizacao) {
        sessaoAutenticacaoService.exigirAdministrador(autorizacao);
        return usuarioService.findAll().stream().map(UsuarioRestController::paraResposta).toList();
    }

    @GetMapping("/{id}")
    public RespostaUsuario findById(@PathVariable Long id, @RequestHeader(value = "Authorization", required = false) String autorizacao) {
        sessaoAutenticacaoService.exigirAdministrador(autorizacao);
        return paraResposta(usuarioService.findById(id));
    }

    @PostMapping
    public ResponseEntity<RespostaUsuario> save(@RequestBody RequisicaoUsuario requisicao,
            @RequestHeader(value = "Authorization", required = false) String autorizacao) {
        sessaoAutenticacaoService.exigirAdministrador(autorizacao);
        Usuario usuario = new Usuario();
        aplicarDados(requisicao, usuario, true);
        Usuario salvo = usuarioService.save(usuario);
        // Informa ao cliente a URL do novo recurso junto com o status HTTP 201.
        return ResponseEntity.created(URI.create("/api/usuarios/" + salvo.getId())).body(paraResposta(salvo));
    }

    @PutMapping("/{id}")
    public RespostaUsuario update(@PathVariable Long id, @RequestBody RequisicaoUsuario requisicao,
            @RequestHeader(value = "Authorization", required = false) String autorizacao) {
        sessaoAutenticacaoService.exigirAdministrador(autorizacao);
        Usuario usuario = usuarioService.findById(id);
        aplicarDados(requisicao, usuario, false);
        usuario.setId(id);
        return paraResposta(usuarioService.update(usuario));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String autorizacao) {
        sessaoAutenticacaoService.exigirAdministrador(autorizacao);
        usuarioService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/sessao")
    public ResponseEntity<Void> encerrarSessao(@RequestHeader(value = "Authorization", required = false) String autorizacao) {
        sessaoAutenticacaoService.revogar(autorizacao);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/login")
    public ResponseEntity<RespostaLogin> autenticar(@RequestBody RequisicaoLogin requisicao) {
        if (requisicao == null || requisicao.login() == null || requisicao.senha() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe login e senha.");
        }
        try {
            Usuario usuario = usuarioService.autenticar(requisicao.login(), requisicao.senha());
            return ResponseEntity.ok(new RespostaLogin(paraResposta(usuario), sessaoAutenticacaoService.criar(usuario)));
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        } catch (RuntimeException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Login ou senha inválidos.", exception);
        }
    }

    private void aplicarDados(RequisicaoUsuario requisicao, Usuario usuario, boolean criando) {
        if (requisicao == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O corpo da requisição é obrigatório.");
        }
        usuario.setLogin(requisicao.login());
        usuario.setPerfil(requisicao.perfil());
        if (requisicao.senha() != null && !requisicao.senha().isBlank()) {
            usuario.setSenha(requisicao.senha());
        } else if (criando) {
            usuario.setSenha(null);
        }
        if (requisicao.pessoaId() != null) {
            Pessoa pessoa = pessoaService.findById(requisicao.pessoaId());
            usuario.setPessoa(pessoa);
        } else if (criando) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "pessoaId é obrigatório.");
        }
    }

    private static RespostaUsuario paraResposta(Usuario usuario) {
        Pessoa pessoa = usuario.getPessoa();
        ResumoPessoa resumo = pessoa == null ? null : new ResumoPessoa(pessoa.getId(), pessoa.getNome());
        return new RespostaUsuario(usuario.getId(), usuario.getLogin(), usuario.getPerfil(),
                Boolean.TRUE.equals(usuario.getAtivo()), resumo);
    }

    public record RequisicaoLogin(String login, String senha) { }
    public record RequisicaoUsuario(String login, String senha, String perfil, Long pessoaId) { }
    public record ResumoPessoa(Long id, String nome) { }
    public record RespostaUsuario(Long id, String login, String perfil, boolean ativo, ResumoPessoa pessoa) { }
    public record RespostaLogin(RespostaUsuario usuario, String token) { }
}


