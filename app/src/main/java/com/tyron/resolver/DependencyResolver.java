package com.tyron.resolver;
import com.tyron.resolver.model.Dependency;
import java.util.List;
import java.util.ArrayList;
import java.io.InputStream;
import java.net.URL;
import java.net.MalformedURLException;
import java.io.IOException;
import com.tyron.resolver.parser.POMParser;
import java.util.Collection;
import java.util.Collections;
import org.xmlpull.v1.XmlPullParserException;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

public class DependencyResolver {
	
	private static final String TAG = DependencyResolver.class.getSimpleName();
	private static final List<String> REPOS = List.of(
		"https://repo1.maven.org/maven2",
		"https://maven.google.com"
	);
	private final ExecutorService service = Executors.newFixedThreadPool(4);
	private final Handler mainThread = new Handler(Looper.getMainLooper());
	private final Dependency library;

	public DependencyResolver(Dependency library) {
		this.library = library;
	}

	public void resolve(final ResolveTask<List<Dependency>> task) {
		final List<Dependency> dependencies = new ArrayList<>();
		
		service.execute(new Runnable() {
			@Override
			public void run() {
				dependencies.addAll(resolveDependency(library));
				int size = dependencies.size();
				mainThread.post(new Runnable() {
					@Override
					public void run() {
						task.onResult(dependencies);
					}
				});
			}
		});
	}
	
	private Set<Dependency> resolvedDependencies = new HashSet<>();

	private Set<Dependency> resolveDependency(Dependency parent) {
		if (resolvedDependencies.contains(parent)) {
			Log.d(TAG, "Skipping " + parent + " to avoid circular dependency");
			return Collections.emptySet();
		}
		
		InputStream is = getInputStream(parent);

		if (is == null) {
			return Collections.emptySet();
		}
		
		
		Log.d(TAG, "Resolving dependency: " + parent);
		
		Set<Dependency> dependencies = new HashSet<>();

		POMParser parser = new POMParser();
		try {
			for (Dependency dep : parser.parse(is)) {
				// skip test dependencies as its not important for now
				if (dep.getScope() != null && dep.getScope().equals("test")) {
					continue;
				}
				dependencies.add(dep);
			}
		} catch (IOException | XmlPullParserException e) {
			Log.d(TAG, "Failed to resolve " + parent + ", " + e.getMessage());
		} 
		
		for (Dependency dep : new HashSet<>(dependencies)) {
			if (dep.getScope() != null && dep.getScope().equals("test")) {
				continue;
			}
			dependencies.addAll(resolveDependency(dep));
		}
		
		resolvedDependencies.addAll(dependencies);
		
		try {
			is.close();
		} catch (IOException e) {}

		return dependencies;
	}

	public static InputStream getInputStream(Dependency dependency) {
		String pomPath = dependency.getPomDownloadLink();

		for (String url : REPOS) {
			try {
				URL downloadUrl = new URL(url + "/" + pomPath);

				InputStream is = downloadUrl.openStream();
				if (is != null) {
					return is;
				}
			} catch (MalformedURLException | IOException e) {
				continue;
			}
		}
		return null;
	}
}
