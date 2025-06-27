package com.kts.kronos.domain.model.roles;

/*
Representa permissão de um usuário
usando sealed interface para restringir quem pode implementar a role
* */
public sealed interface Role permits CtoRole, ManagerRole, PartnerRole { }
