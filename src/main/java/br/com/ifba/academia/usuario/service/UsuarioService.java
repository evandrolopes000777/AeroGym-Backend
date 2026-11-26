package br.com.ifba.academia.usuario.service;

import br.com.ifba.academia.usuario.entity.Usuario;
import br.com.ifba.academia.usuario.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class UsuarioService implements UsuarioIService {
    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder codificadorSenha;

    @Override
    public Usuario save(Usuario usuario) {
        if (usuario == null) throw new IllegalArgumentException("Os dados do usuario nao podem ser nulos");
        if (usuario.getId() != null) throw new IllegalArgumentException("O ID do usuario deve ser nulo");
        if (usuario.getLogin() == null || usuario.getLogin().isBlank()) throw new IllegalArgumentException("O campo login e obrigatorio");
        if (usuario.getSenha() == null || usuario.getSenha().isBlank()) throw new IllegalArgumentException("O campo senha e obrigatorio");
        if (!perfilValido(usuario.getPerfil())) throw new IllegalArgumentException("O perfil deve ser ADMIN ou ALUNO");
        if (usuario.getPessoa() == null || usuario.getPessoa().getId() == null) throw new IllegalArgumentException("O usuario deve estar vinculado a uma pessoa previamente cadastrada");
        if (usuarioRepository.existsByLogin(usuario.getLogin())) throw new RuntimeException("Ja existe um usuario cadastrado com este login");

        usuario.setSenha(codificadorSenha.encode(usuario.getSenha()));
        usuario.setAtivo(true);
        return usuarioRepository.save(usuario);
    }

    @Override
    public Usuario update(Usuario usuario) {
        if (usuario == null) throw new IllegalArgumentException("Os dados do usuario nao podem ser nulos");
        if (usuario.getId() == null) throw new IllegalArgumentException("O ID e obrigatorio para atualizar o usuario");
        if (!usuarioRepository.existsById(usuario.getId())) throw new RuntimeException("Usuario nao encontrado");
        if (usuario.getLogin() == null || usuario.getLogin().isBlank()) throw new IllegalArgumentException("O campo login e obrigatorio");
        if (!perfilValido(usuario.getPerfil())) throw new IllegalArgumentException("O perfil deve ser ADMIN ou ALUNO");
        if (usuario.getSenha() != null && !usuario.getSenha().isBlank() && !usuario.getSenha().startsWith("$2")) {
            usuario.setSenha(codificadorSenha.encode(usuario.getSenha()));
        }
        return usuarioRepository.save(usuario);
    }

    @Override
    public void delete(Long id) {
        if (id == null) throw new IllegalArgumentException("O ID nao pode ser nulo para remocao");
        Usuario usuario = usuarioRepository.findById(id).orElseThrow(() -> new RuntimeException("Usuario nao encontrado"));
        if (!Boolean.TRUE.equals(usuario.getAtivo())) throw new RuntimeException("Este usuario ja consta como desativado no sistema");
        usuario.setAtivo(false);
        usuarioRepository.save(usuario);
    }

    @Override
    public Usuario findById(Long id) {
        if (id == null) throw new IllegalArgumentException("O ID nao pode ser nulo");
        return usuarioRepository.findById(id).orElseThrow(() -> new RuntimeException("Usuario nao encontrado"));
    }

    @Override
    public List<Usuario> findAll() {
        return usuarioRepository.findByAtivoTrue();
    }

    @Override
    public Usuario autenticar(String login, String senha) {
        if (login == null || login.isBlank()) throw new IllegalArgumentException("O login deve ser informado");
        if (senha == null || senha.isBlank()) throw new IllegalArgumentException("A senha deve ser informada");
        Optional<Usuario> usuarioOpt = usuarioRepository.findByLogin(login);
        if (usuarioOpt.isEmpty()) throw new RuntimeException("Credenciais invalidas");
        Usuario usuario = usuarioOpt.get();
        if (!Boolean.TRUE.equals(usuario.getAtivo())) throw new RuntimeException("Acesso negado: usuario inativo");
        String senhaArmazenada = usuario.getSenha();
        boolean senhaValida = senhaArmazenada != null && senhaArmazenada.startsWith("$2")
                ? codificadorSenha.matches(senha, senhaArmazenada) : senha.equals(senhaArmazenada);
        if (!senhaValida) throw new RuntimeException("Credenciais invalidas");
        if (senhaArmazenada != null && !senhaArmazenada.startsWith("$2")) {
            usuario.setSenha(codificadorSenha.encode(senha));
            usuarioRepository.save(usuario);
        }
        return usuario;
    }

    private boolean perfilValido(String perfil) {
        return perfil != null && (perfil.equalsIgnoreCase("ADMIN") || perfil.equalsIgnoreCase("ALUNO"));
    }
}
