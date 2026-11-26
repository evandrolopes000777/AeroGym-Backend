package br.com.ifba.academia.infrastructure.config;

import br.com.ifba.academia.pessoa.entity.Pessoa;
import br.com.ifba.academia.pessoa.service.PessoaIService;
import br.com.ifba.academia.usuario.entity.Usuario;
import br.com.ifba.academia.usuario.service.UsuarioIService;
import br.com.ifba.academia.usuario.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class InicializadorAdministrador implements CommandLineRunner {
    private final UsuarioIService usuarioService;
    @Autowired
    private UsuarioRepository usuarioRepository;
    private final PessoaIService pessoaService;
    private final String login;
    private final String senha;
    private final String cpf;

    public InicializadorAdministrador(
            UsuarioIService usuarioService,
            PessoaIService pessoaService,
            @Value("${aerogym.bootstrap.admin.login:}") String login,
            @Value("${aerogym.bootstrap.admin.password:}") String senha,
            @Value("${aerogym.bootstrap.admin.cpf:}") String cpf) {
        this.usuarioService = usuarioService;
        this.pessoaService = pessoaService;
        this.login = login;
        this.senha = senha;
        this.cpf = cpf;
    }

    @Override
    public void run(String... args) {
        if (login.isBlank() || senha.isBlank() || cpf.isBlank() || usuarioRepository.count() > 0) {
            return;
        }

        Pessoa pessoa = new Pessoa();
        pessoa.setNome("Administrador AeroGym");
        pessoa.setCpf(cpf);
        pessoa.setDataNascimento(LocalDate.of(2000, 1, 1));
        pessoa.setIsAdministrador(true);
        pessoa = pessoaService.save(pessoa);

        Usuario administrador = new Usuario();
        administrador.setLogin(login);
        administrador.setSenha(senha);
        administrador.setPerfil("ADMIN");
        administrador.setPessoa(pessoa);
        usuarioService.save(administrador);
    }
}


