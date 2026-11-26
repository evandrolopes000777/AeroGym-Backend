# AeroGym — Backend

Backend do AeroGym, sistema acadêmico de gestão de academia. A aplicação combina os módulos de domínio existentes com uma API REST Spring Boot para autenticação e gerenciamento de usuários.

## Arquitetura

A API segue uma organização em camadas: controllers recebem as requisições HTTP, services concentram as regras de negócio, repositories acessam os dados e entities representam o modelo persistido. Os contratos da API usam DTOs para controlar os dados enviados e recebidos.

## Recursos REST

| Método | Rota | Recurso |
|---|---|---|
| `POST` | `/api/usuarios/login` | Autentica um usuário e inicia uma sessão |
| `GET` | `/api/usuarios` | Lista usuários ativos |
| `GET` | `/api/usuarios/{id}` | Consulta um usuário |
| `POST` | `/api/usuarios` | Cadastra um usuário vinculado a uma pessoa |
| `PUT` | `/api/usuarios/{id}` | Atualiza os dados de um usuário |
| `DELETE` | `/api/usuarios/{id}` | Desativa um usuário |
| `DELETE` | `/api/usuarios/sessao` | Encerra a sessão atual |

As operações de gerenciamento exigem perfil administrador. As senhas são armazenadas com BCrypt; as sessões temporárias usam tokens e expiram após 30 minutos.

## Tecnologias

Java 17, Spring Boot, Spring Web, Spring Data JPA e PostgreSQL.

## Escopo atual

A API REST publicada neste projeto cobre autenticação e gerenciamento de usuários. Os módulos de pessoas, planos, pagamentos, exercícios, fichas de treino e controle de acesso ainda não estão expostos como endpoints REST. As telas Swing permanecem no código do projeto.

O frontend React está no repositório [AeroGym-React](https://github.com/evandrolopes000777/AeroGym-React).
