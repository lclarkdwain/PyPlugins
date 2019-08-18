package dev.anullihate;

import cn.nukkit.plugin.PluginBase;
import cn.nukkit.plugin.PluginLoadOrder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

public class PyPlugins extends PluginBase {

    private static PyPlugins instance = null;

    public static PyPlugins getInstance() {
        return instance;
    }

    public void onLoad() {
        if (!new File("lib/jython-standalone.jar").exists()) {
            getServer().getLogger().critical("Could not find lib/jython-standalone.jar!");
            try {
                URL jythonRepo = new URL("https://repo1.maven.org/maven2/org/python/jython-standalone/2.7.1/jython-standalone-2.7.1.jar");
                URLConnection connection = jythonRepo.openConnection();
                connection.connect();

                File libFolder = new File("lib");
                if (!libFolder.exists()) {
                    libFolder.mkdir();
                }

                InputStream inputStream = connection.getInputStream();
                FileOutputStream fileOutputStream = new FileOutputStream(new File("lib/jython-standalone.jar"));

                long total = connection.getContentLengthLong();
                long progress = 0;
                byte[] buffer = new byte[1024];
                int read;
                long start = System.currentTimeMillis();

                while ((read = inputStream.read(buffer)) != -1) {
                    fileOutputStream.write(buffer, 0, read);
                    progress += read;
                    if (System.currentTimeMillis() - start > 2000) {
                        getServer().getLogger().warning("Downloading jython-standalone: " + (progress * 100 / total) + " %");
                        start = System.currentTimeMillis();
                    }
                }

                fileOutputStream.close();
                inputStream.close();

                getServer().getLogger().info("Download Successful");
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            getServer().getLogger().info("lib/jython-standalone.jar found!");
        }
    }

    public void onEnable() {
        this.getServer().getPluginManager().registerInterface(PyPluginLoader.class);
        List<String> loaders = new ArrayList<>();
        loaders.add(PyPluginLoader.class.getName());
        this.getServer().getPluginManager().loadPlugins(new File(this.getServer().getPluginPath()), loaders, true);
        this.getServer().enablePlugins(PluginLoadOrder.STARTUP);
    }
}
