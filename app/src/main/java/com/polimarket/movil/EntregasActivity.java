package com.polimarket.movil;

import android.os.Bundle;
import android.graphics.Color;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import com.polimarket.movil.data.OperacionesApi;
import org.json.*;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.text.ParsePosition;
import java.util.Date;
import java.util.TimeZone;
import java.util.Locale;

public class EntregasActivity extends PantallaActivity {
    private LinearLayout lista;
    private OperacionesApi api;
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); setTitle("Entregas");
        texto("Pedidos pendientes",24);
        texto("Pedidos de todos los vendedores. Confirmar el despacho registra la entrega y descuenta su inventario.",16);
        api = new OperacionesApi(getString(R.string.server_url));
        boton("Actualizar pedidos").setOnClickListener(v -> cargar(null));
        lista = new LinearLayout(this); lista.setOrientation(LinearLayout.VERTICAL);
        contenido.addView(lista);
        cargar(null);
    }
    private void cargar(String mensaje) {
        ejecutar(() -> new JSONArray(api.solicitar("/api/entregas/pedidos/pendientes",false)), pedidos -> {
            lista.removeAllViews();
            estado.setText(mensaje != null ? mensaje : pedidos.length()==0 ? "No hay pedidos pendientes." : "Pedidos pendientes, del m\u00e1s antiguo al m\u00e1s reciente.");
            for(int i=0;i<pedidos.length();i++) {
                try { agregar(pedidos.getJSONObject(i)); }
                catch(JSONException error) { estado.setText(R.string.response_error); }
            }
        });
    }
    private void agregar(JSONObject pedido) throws JSONException {
        long id = pedido.getLong("id");
        String cliente = pedido.getString("cliente");
        String direccion = pedido.optString("direccion","");
        String fechaSolicitud = formatearFecha(pedido.optString("fecha", ""));
        JSONArray detalles = pedido.getJSONArray("detalles");
        StringBuilder resumen = new StringBuilder("Pedido #"+id+"\nCliente: "+cliente+"\nDirecci\u00f3n: "+direccion+"\nFecha de solicitud: "+fechaSolicitud+"\n");
        for(int i=0;i<detalles.length();i++) {
            JSONObject detalle = detalles.getJSONObject(i);
            resumen.append("\n\u2022 ").append(detalle.getString("nombre")).append(" \u00d7 ").append(detalle.getInt("cantidad"));
        }
        resumen.append("\n\nTotal: ").append(NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-CO")).format(pedido.getDouble("total")));
        TextView texto = new TextView(this); texto.setText(resumen.toString());
        texto.setTextColor(Color.BLACK);
        texto.setIncludeFontPadding(true);
        texto.setLineSpacing(4 * getResources().getDisplayMetrics().density, 1.1f);
        texto.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        texto.setTextSize(18); texto.setPadding(0,24,0,12); lista.addView(texto);
        Button registrar = new Button(this); registrar.setText("Registrar entrega y salida #"+id); lista.addView(registrar);
        registrar.setOnClickListener(v -> {
            if(ocupado) return;
            new AlertDialog.Builder(this).setTitle("Confirmar despacho #"+id)
                .setMessage(resumen+"\n\nSe descontar\u00e1n estos productos del inventario. Esta acci\u00f3n registra el despacho, no la recepci\u00f3n del cliente.")
                .setNegativeButton("Cancelar",null)
                .setPositiveButton("Registrar salida",(dialog,which)-> ejecutar(
                    () -> new JSONObject(api.solicitar("/api/entregas/pedidos/"+id,true)),
                    entrega -> cargar("Entrega registrada para el pedido #"+id+". Inventario actualizado.")))
                .show();
        });
    }
    private String formatearFecha(String fecha) {
        if (fecha == null || fecha.isEmpty() || "null".equals(fecha)) return "No registrada";
        // El backend envia LocalDateTime: preservar su fecha y hora sin convertir zona.
        String patron = fecha.length() >= 19 ? "yyyy-MM-dd'T'HH:mm:ss" : "yyyy-MM-dd'T'HH:mm";
        int longitud = fecha.length() >= 19 ? 19 : 16;
        if (fecha.length() < longitud) return "Fecha no disponible";
        SimpleDateFormat entrada = new SimpleDateFormat(patron, Locale.ROOT);
        entrada.setLenient(false);
        entrada.setTimeZone(TimeZone.getTimeZone("UTC"));
        ParsePosition posicion = new ParsePosition(0);
        Date valor = entrada.parse(fecha.substring(0, longitud), posicion);
        if (valor == null || posicion.getIndex() != longitud) return "Fecha no disponible";
        SimpleDateFormat salida = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.forLanguageTag("es-CO"));
        salida.setTimeZone(TimeZone.getTimeZone("UTC"));
        return salida.format(valor);
    }

}
