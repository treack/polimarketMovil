package com.polimarket.movil.data;

import org.json.JSONException;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/** Cliente REST de Bodega. No accede directamente a MySQL. */
public final class InventarioApi {
    private final String baseUrl;

    public InventarioApi(String baseUrl) { this.baseUrl = baseUrl; }

    public Disponibilidad consultarDisponibilidad(long productoId, int cantidad) throws IOException, JSONException {
        URL url = new URL(baseUrl + "/api/bodega/productos/" + productoId
                + "/disponibilidad?cantidad=" + cantidad);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        try {
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(15000);
            connection.setInstanceFollowRedirects(false);
            connection.setRequestProperty("Accept", "application/json");
            int status = connection.getResponseCode();
            if (status != HttpURLConnection.HTTP_OK) throw new ApiException(status);
            try (InputStream input = connection.getInputStream();
                 ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[2048];
                int read;
                while ((read = input.read(buffer)) != -1) {
                    if (output.size() + read > 65536) throw new IOException("Respuesta demasiado grande");
                    output.write(buffer, 0, read);
                }
                JSONObject result = new JSONObject(new String(output.toByteArray(), StandardCharsets.UTF_8));
                Object disponible = result.get("disponible");
                if (!(disponible instanceof Boolean) || result.getLong("productoId") != productoId
                        || result.getInt("cantidadSolicitada") != cantidad)
                    throw new JSONException("Respuesta de disponibilidad invalida");
                return new Disponibilidad((Boolean) disponible, result.isNull("nombreProducto") ? null : result.optString("nombreProducto", null));
            }
        } finally { connection.disconnect(); }
    }

    public static final class Disponibilidad {
        public final boolean disponible;
        public final String nombreProducto;
        public Disponibilidad(boolean disponible, String nombreProducto) {
            this.disponible = disponible;
            this.nombreProducto = nombreProducto;
        }
    }

    public static final class ApiException extends IOException {
        public final int status;
        public ApiException(int status) { super("HTTP " + status); this.status = status; }
    }
}
