package br.com.ifba.academia.usuario.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.ifba.academia.pessoa.entity.Pessoa;
import br.com.ifba.academia.pessoa.service.PessoaIService;
import br.com.ifba.academia.usuario.entity.Usuario;
import br.com.ifba.academia.usuario.service.SessaoAutenticacaoService;
import br.com.ifba.academia.usuario.service.UsuarioIService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class TesteUsuarioRestController {
    @Mock private UsuarioIService usuarioService;
    @Mock private PessoaIService pessoaService;
    @Mock private SessaoAutenticacaoService sessaoAutenticacaoService;
    private UsuarioRestController controlador;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        controlador = new UsuarioRestController(usuarioService, pessoaService, sessaoAutenticacaoService);
    }

    @Test
    void listaUsuariosAtivos() {
        when(usuarioService.findAll()).thenReturn(List.of(usuario(1L, "ana", "ALUNO")));
        var resposta = controlador.findAll("Bearer token");
        assertEquals(1, resposta.size());
        assertEquals("ana", resposta.get(0).login());
        verify(sessaoAutenticacaoService).exigirAdministrador("Bearer token");
    }

    @Test
    void bloqueiaListagemQuandoSessaoNaoEAdministrador() {
        doThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED))
                .when(sessaoAutenticacaoService).exigirAdministrador("Bearer invalido");

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> controlador.findAll("Bearer invalido"));

        assertEquals(HttpStatus.UNAUTHORIZED, erro.getStatusCode());
    }

    @Test
    void buscaUsuarioPorId() {
        when(usuarioService.findById(1L)).thenReturn(usuario(1L, "admin", "ADMIN"));
        assertEquals(1L, controlador.findById(1L, "Bearer token").id());
    }

    @Test
    void criaUsuarioEDevolve201() {
        when(pessoaService.findById(8L)).thenReturn(pessoa());
        when(usuarioService.save(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario valor = invocation.getArgument(0);
            valor.setId(12L);
            return valor;
        });
        var resposta = controlador.save(new UsuarioRestController.RequisicaoUsuario("aluno", "senha", "ALUNO", 8L), "Bearer token");
        assertEquals(HttpStatus.CREATED, resposta.getStatusCode());
        assertNotNull(resposta.getBody());
        assertEquals(12L, resposta.getBody().id());
        assertEquals("/api/usuarios/12", resposta.getHeaders().getLocation().toString());
        verify(usuarioService).save(any(Usuario.class));
    }

    @Test
    void rejeitaCadastroSemPessoaVinculada() {
        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> controlador.save(
                        new UsuarioRestController.RequisicaoUsuario("aluno", "senha", "ALUNO", null),
                        "Bearer token"));

        assertEquals(HttpStatus.BAD_REQUEST, erro.getStatusCode());
    }

    @Test
    void atualizaUsuario() {
        Usuario atual = usuario(12L, "antigo", "ALUNO");
        when(usuarioService.findById(12L)).thenReturn(atual);
        when(pessoaService.findById(8L)).thenReturn(pessoa());
        when(usuarioService.update(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));
        var resposta = controlador.update(12L, new UsuarioRestController.RequisicaoUsuario("novo", null, "ALUNO", 8L), "Bearer token");
        assertEquals("novo", resposta.login());
        verify(usuarioService).update(atual);
    }

    @Test
    void desativaUsuarioEDevolve204() {
        doNothing().when(usuarioService).delete(5L);
        var resposta = controlador.delete(5L, "Bearer token");
        assertEquals(HttpStatus.NO_CONTENT, resposta.getStatusCode());
        verify(usuarioService).delete(5L);
    }

    @Test
    void encerraSessaoEDevolve204() {
        var resposta = controlador.encerrarSessao("Bearer token");
        assertEquals(HttpStatus.NO_CONTENT, resposta.getStatusCode());
        verify(sessaoAutenticacaoService).revogar("Bearer token");
    }

    @Test
    void autenticaESemIncluirSenhaNaResposta() {
        Usuario administrador = usuario(1L, "admin", "ADMIN");
        when(usuarioService.autenticar("admin", "senha")).thenReturn(administrador);
        when(sessaoAutenticacaoService.criar(administrador)).thenReturn("token");
        var resposta = controlador.autenticar(new UsuarioRestController.RequisicaoLogin("admin", "senha"));
        assertEquals(HttpStatus.OK, resposta.getStatusCode());
        assertNotNull(resposta.getBody());
        assertEquals("ADMIN", resposta.getBody().usuario().perfil());
        assertEquals("token", resposta.getBody().token());
    }

    @Test
    void rejeitaLoginSemCamposObrigatorios() {
        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> controlador.autenticar(new UsuarioRestController.RequisicaoLogin(null, "senha")));

        assertEquals(HttpStatus.BAD_REQUEST, erro.getStatusCode());
    }

    @Test
    void credenciaisInvalidasRetornamNaoAutorizado() {
        when(usuarioService.autenticar("admin", "incorreta"))
                .thenThrow(new RuntimeException("Credenciais invalidas"));

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> controlador.autenticar(new UsuarioRestController.RequisicaoLogin("admin", "incorreta")));

        assertEquals(HttpStatus.UNAUTHORIZED, erro.getStatusCode());
    }

    private Usuario usuario(Long id, String login, String perfil) {
        Usuario valor = new Usuario();
        valor.setId(id);
        valor.setLogin(login);
        valor.setSenha("senha-interna");
        valor.setPerfil(perfil);
        valor.setAtivo(true);
        valor.setPessoa(pessoa());
        return valor;
    }

    private Pessoa pessoa() {
        Pessoa valor = new Pessoa();
        valor.setId(8L);
        valor.setNome("Ana Souza");
        return valor;
    }
}

