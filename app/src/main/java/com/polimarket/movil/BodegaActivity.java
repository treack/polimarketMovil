package com.polimarket.movil;

import android.content.Intent;
import android.os.Bundle;

public class BodegaActivity extends PantallaActivity {
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); setTitle("Bodega");
        texto("Bodega",24);
        boton("Consultar disponibilidad").setOnClickListener(v ->
                startActivity(new Intent(this,DisponibilidadActivity.class)));
        boton("Consultar proveedores").setOnClickListener(v ->
                startActivity(new Intent(this,ProveedoresActivity.class)));
        texto("Las consultas no reservan ni descuentan inventario.",16);
    }
}
