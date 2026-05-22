package com.ecole221.anneeacademique.service.domain.model;

import com.ecole221.anneeacademique.service.domain.valuobject.CodeAnnee;
import com.ecole221.common.valueobject.BaseId;

public class AnneeAcademiqueId extends BaseId<CodeAnnee> {
    public AnneeAcademiqueId(CodeAnnee value) {
        super(value);
    }
}
