package com.polimarket.movil;

import android.content.Intent;
import android.os.Bundle;

public class MainActivity extends PantallaActivity {
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setTitle("PoliMarket Móvil");
        texto("Bodega y Entregas",24);
        texto("Selecciona el área en la que vas a trabajar.",16);
        boton("Bodega").setOnClickListener(v -> startActivity(new Intent(this,BodegaActivity.class)));
        texto("Disponibilidad de productos y proveedores.",16);
        boton("Entregas").setOnClickListener(v -> startActivity(new Intent(this,EntregasActivity.class)));
        texto("Pedidos pendientes, detalle de productos y registro de despacho.",16);
    }
}
