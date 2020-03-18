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

    public void onEnable() {
        this.getServer().getPluginManager().registerInterface(PyPluginLoader.class);
        List<String> loaders = new ArrayList<>();
        loaders.add(PyPluginLoader.class.getName());
        this.getServer().getPluginManager().loadPlugins(new File(this.getServer().getPluginPath()), loaders, true);
        this.getServer().enablePlugins(PluginLoadOrder.STARTUP);
    }
}
