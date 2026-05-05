package com.example.assinador.assinador;

import com.example.assinador.API.AssinadorRequest;
import com.example.assinador.API.AssinadorResponse;

public interface ISignService {
    AssinadorResponse sign(AssinadorRequest request);
} 