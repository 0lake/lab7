package com.general.models.base;

import lombok.Data;

import java.io.Serializable;

@Data
public abstract class Element implements Validatable, Serializable {
    private Long id;
    private String username;

    public abstract String getName();
}
