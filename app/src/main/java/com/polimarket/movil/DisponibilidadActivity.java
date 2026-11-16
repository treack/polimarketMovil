package com.polimarket.movil;

import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import com.polimarket.movil.data.InventarioApi;
import org.json.JSONException;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DisponibilidadActivity extends AppCompatActivity {
    private static final String LOCAL_NETWORK = "android.permission.ACCESS_LOCAL_NETWORK";
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private EditText productId;
    private EditText quantity;
    private Button check;
    private TextView result;
    private ProgressBar loading;
    private InventarioApi api;
    private long inputVersion = 0;

    private final ActivityResultLauncher<String> localPermission = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) consultar();
                else mostrar(getString(R.string.local_permission_denied), false);
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.enableEdgeToEdge(getWindow());
        setTitle("Bodega · Disponibilidad");
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
        productId = findViewById(R.id.product_id);
        quantity = findViewById(R.id.quantity);
        check = findViewById(R.id.check_button);
        result = findViewById(R.id.result);
        loading = findViewById(R.id.loading);
        api = new InventarioApi(getString(R.string.server_url));
        TextWatcher watcher = new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                inputVersion++;
                result.setText(R.string.initial_result);
                result.setTextColor(Color.parseColor("#18372C"));
            }
            public void afterTextChanged(Editable s) { }
        };
        productId.addTextChangedListener(watcher);
        quantity.addTextChangedListener(watcher);
        check.setOnClickListener(v -> consultar());
    }

    private void consultar() {
        long id;
        int amount;
        try {
            id = Long.parseLong(productId.getText().toString().trim());
            if (id <= 0) throw new NumberFormatException();
        } catch (NumberFormatException error) {
            productId.setError(getString(R.string.invalid_product)); productId.requestFocus(); return;
        }
        try {
            amount = Integer.parseInt(quantity.getText().toString().trim());
            if (amount <= 0) throw new NumberFormatException();
        } catch (NumberFormatException error) {
            quantity.setError(getString(R.string.invalid_quantity)); quantity.requestFocus(); return;
        }
        if (Build.VERSION.SDK_INT >= 37 && checkSelfPermission(LOCAL_NETWORK) != PackageManager.PERMISSION_GRANTED) {
            localPermission.launch(LOCAL_NETWORK); return;
        }
        long version = ++inputVersion;
        check.setEnabled(false);
        loading.setVisibility(View.VISIBLE);
        result.setText(R.string.loading);
        executor.execute(() -> {
            String message;
            boolean success = false;
            try {
                InventarioApi.Disponibilidad disponibilidad = api.consultarDisponibilidad(id, amount);
                success = disponibilidad.disponible;
                String producto = disponibilidad.nombreProducto;
                if (producto == null || producto.trim().isEmpty()) producto = getString(R.string.product_fallback, id);
                message = getString(disponibilidad.disponible ? R.string.available : R.string.unavailable, amount, producto);
            } catch (InventarioApi.ApiException error) {
                message = getString(R.string.server_error, error.status);
            } catch (JSONException error) {
                message = getString(R.string.response_error);
            } catch (IOException error) {
                message = getString(R.string.network_error);
            }
            String finalMessage = message;
            boolean finalSuccess = success;
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                check.setEnabled(true);
                loading.setVisibility(View.GONE);
                if (version == inputVersion) mostrar(finalMessage, finalSuccess);
            });
        });
    }

    private void mostrar(String message, boolean success) {
        result.setText(message);
        result.setTextColor(Color.parseColor(success ? "#246B4E" : "#A12E2E"));
    }

    @Override
    protected void onDestroy() {
        inputVersion++;
        executor.shutdownNow();
        super.onDestroy();
    }
}
