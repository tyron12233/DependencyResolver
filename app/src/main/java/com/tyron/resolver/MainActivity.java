package com.tyron.resolver;

import android.app.*;
import android.os.*;
import com.tyron.resolver.parser.POMParser;
import java.util.List;
import com.tyron.resolver.model.Dependency;
import java.util.ArrayList;
import java.io.ByteArrayInputStream;
import android.widget.Toast;
import java.io.InputStream;
import java.io.StringReader;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;
import android.widget.Button;
import android.widget.EditText;
import android.view.View;
import android.widget.ListView;
import java.util.function.Function;
import java.util.stream.Collectors;
import android.widget.ArrayAdapter;

public class MainActivity extends Activity {

	final List<Dependency> dependencies = new ArrayList<>();
	
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
		
		final Button btn_resolve = findViewById(R.id.btn_resolve); 
		final EditText et = findViewById(R.id.edittext);
		
		final ListView listView = findViewById(R.id.listView);
		
		btn_resolve.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Dependency dep = null;
				try {
					dep = Dependency.from(et.getText().toString());
				} catch (IllegalArgumentException e) {
					showToast(e.getMessage());
					return;
				}
				
				btn_resolve.setText("Resolving...");
				
				DependencyResolver resolver = new DependencyResolver(dep);
				resolver.resolve(new ResolveTask<List<Dependency>>() {
					@Override
					public void onResult(List<Dependency> result) {
						List<String> deps = result.stream().map(new Function<Dependency, String>() {
							@Override
							public String apply(Dependency dep) {
								return dep.toString();
							}
						}).collect(Collectors.toList());
						
						ArrayAdapter adapter = new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_list_item_1, deps);
						listView.setAdapter(adapter);
						btn_resolve.setText("Resolved " + result.size());
					}
				});
			}
		});
		
    }
	
	private void showToast(String string) {
		Toast.makeText(this, string, Toast.LENGTH_LONG).show();
	}
	
}
