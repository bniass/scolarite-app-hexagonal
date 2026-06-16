package com.ecole221.inscription.service.application.command;

import java.util.UUID;

public record ChangerClasseCommand(UUID inscriptionId, UUID nouvelleClasseId) {}
