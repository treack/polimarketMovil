package com.polimarket.movil.data;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import org.json.JSONObject;

/** Transporte REST para consultas de proveedores y operaciones de Entregas. */
public final class OperacionesApi {
    private final String servidor;
    public OperacionesApi(String servidor) { this.servidor = servidor; }
    public String solicitar(String ruta, boolean post) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(servidor+ruta).openConnection();
        try {
            connection.setConnectTimeout(10000); connection.setReadTimeout(15000);
            connection.setInstanceFollowRedirects(false);
            connection.setRequestProperty("Accept","application/json");
            connection.setRequestMethod(post ? "POST" : "GET");
            int code = connection.getResponseCode();
            InputStream stream = code>=200 && code<300 ? connection.getInputStream() : connection.getErrorStream();
            String body = "";
            if(stream != null) try(InputStream input=stream; ByteArrayOutputStream output=new ByteArrayOutputStream()) {
                byte[] buffer=new byte[4096]; int count;
                while((count=input.read(buffer))!=-1) {
                    if(output.size()+count>2097152) throw new IOException("La respuesta del servidor es demasiado grande.");
                    output.write(buffer,0,count);
                }
                body=new String(output.toByteArray(),StandardCharsets.UTF_8);
            }
            if(code<200 || code>=300) {
                String detail = "No se pudo completar la operación (HTTP "+code+").";
                try { detail = new JSONObject(body).optString("detail",detail); } catch(Exception ignored) { }
                throw new IOException(detail);
            }
            return body;
        } catch(SocketTimeoutException | ConnectException | UnknownHostException error) {
            throw new IOException(post ? "No se pudo confirmar el resultado. Actualiza los pedidos antes de volver a registrar la salida."
                    : "No fue posible conectar. Verifica la red y que el backend esté activo en el puerto 5050.",error);
        } finally { connection.disconnect(); }
    }
}
