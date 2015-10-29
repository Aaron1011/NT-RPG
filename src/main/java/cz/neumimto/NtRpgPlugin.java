/*    
 *     Copyright (c) 2015, NeumimTo https://github.com/NeumimTo
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *     
 */

package cz.neumimto;

import com.google.inject.Inject;
import cz.neumimto.configuration.ConfigMapper;
import cz.neumimto.configuration.Settings;
import cz.neumimto.ioc.IoC;
import cz.neumimto.utils.FileUtils;
import djxy.api.MinecraftGuiService;
import org.slf4j.Logger;
import org.spongepowered.api.Game;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GamePostInitializationEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginContainer;

import javax.persistence.EntityManager;
import javax.persistence.Persistence;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Properties;
import java.util.jar.JarFile;

/**
 * Created by NeumimTo on 29.4.2015.
 */
@Plugin(id = "NtRpg", name = "NtRpg", dependencies = "after:MinecraftGUIServer")
public class NtRpgPlugin {
    public static String workingDir;
    public static JarFile pluginjar;
    private static String configPath = File.separator + "mods" + File.separator + "NtRpg";

    @Inject
    public Logger logger;

    public static GlobalScope GlobalScope;


    private EntityManager setupEntityManager(Path p) {
        Properties properties = new Properties();
        try (FileInputStream stream = new FileInputStream(p.toFile())) {
            properties.load(stream);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            Class.forName(properties.getProperty("hibernate.connection.driver_class"));
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        logger.info("Creating EntityManager");
        return Persistence.createEntityManagerFactory("ntrpg", properties).createEntityManager();
    }

    @Listener
    public void onPluginLoad(GamePostInitializationEvent event) {
        long start = System.nanoTime();
        IoC ioc = IoC.get();

        ioc.logger = logger;
        Game game = event.getGame();
        ioc.registerInterfaceImplementation(Game.class, game);
        ioc.registerInterfaceImplementation(Logger.class, logger);
        Optional<PluginContainer> gui = game.getPluginManager().getPlugin("MinecraftGUIServer");
        if (gui.isPresent()) {
            ioc.registerInterfaceImplementation(MinecraftGuiService.class, game.getServiceManager().provide(MinecraftGuiService.class).get());
        } else {
            Settings.ENABLED_GUI = false;
        }
        ioc.registerDependency(this);

        try {
            workingDir = new File(".").getCanonicalPath() + configPath;
            pluginjar = FileUtils.getPluginJar();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Path path = Paths.get(workingDir);
        ConfigMapper.init("NtRpg", path);
        ioc.registerDependency(ConfigMapper.get("NtRpg"));
        try {
            Files.createDirectories(path);
        } catch (IOException e) {
            e.printStackTrace();
        }

        path = Paths.get(workingDir + File.separator + "database.properties");
        if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
            InputStream resourceAsStream = getClass().getClassLoader().getResourceAsStream("database.properties");
            try {
                Files.copy(resourceAsStream, path);
                logger.warn("File \"database.properties\" has been copied into a mods-folder/NtRpg, Configure it and start the server again.");
                game.getServer().shutdown();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        Path p = Paths.get(workingDir + File.separator + "database.properties");
        FileUtils.createFileIfNotExists(p);
        EntityManager em = setupEntityManager(p);
        ioc.registerInterfaceImplementation(EntityManager.class, em);
        ioc.get(IoC.class, ioc);
        ResourceLoader rl = ioc.build(ResourceLoader.class);
        rl.loadJarFile(pluginjar, true);
        GlobalScope = ioc.build(GlobalScope.class);
        rl.loadExternalJars();
        ioc.postProcess();
        double elapsedTime = (System.nanoTime() - start) / 1000000000.0;
        logger.info("NtRpg plugin successfully loaded in " + elapsedTime + " seconds");
    }
}
