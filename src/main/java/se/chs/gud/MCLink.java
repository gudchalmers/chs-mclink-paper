package se.chs.gud;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.bukkit.event.EventHandler;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.Listener;

import net.kyori.adventure.text.Component;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.URI;
import java.net.http.HttpResponse;

import java.util.Objects;
import java.util.UUID;
import java.util.logging.Level;
import java.util.List;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;

public final class MCLink extends JavaPlugin implements Listener {
    private Gson gson = new Gson();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("Successfully Loaded!");
    }

    @Override
    public void onDisable() {
        getLogger().info("Successfully Disabled!");
    }

    @EventHandler
    public void onAsyncPlayerPreLogin(AsyncPlayerPreLoginEvent event) {
        String playerName = event.getName();
        UUID uuid = event.getUniqueId();

        if (isPlayerWhitelisted(uuid)) {
            // unnecessary, but for completeness sake, let's be explicit.
            event.allow();
            getLogger().info(playerName + " was successfully authenticated through whitelist!");
            return;
        }

        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpResponse<String> response;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(Objects.requireNonNull(getConfig().getString("check"))))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString("uuid=" + uuid))
                    .build();
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
            handleLoginResponse(event, playerName, response);
        } catch (Exception e) {
            getLogger().severe("Error: Couldn't complete HTTP request for user: " + playerName);
            getLogger().log(Level.SEVERE, "Stack trace: ", e);
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST,
                    Component.text("An error occurred while authenticating. Please try again."));
        }
    }

    private Boolean isPlayerWhitelisted(UUID uuid) {
        Path currentWorkingDirectory = Paths.get("").toAbsolutePath();
        Path whitelistPath = currentWorkingDirectory.resolve("whitelist.json");

        if (!Files.exists(whitelistPath)) {
            return false;
        }

        try {
            String whitelistJson = Files.readString(whitelistPath);
            List<WhitelistEntry> whitelistEntries = gson.fromJson(whitelistJson, new TypeToken<List<WhitelistEntry>>() {}.getType());

            for (WhitelistEntry entry : whitelistEntries) {
                UUID whitelistUUID = UUID.fromString(entry.getUniqueId()); // Convert the string UUID to java.util.UUID
                if (whitelistUUID.equals(uuid)) {
                    return true;
                }
            }
        } catch (Exception e) {
            getLogger().severe("Error: Couldn't open whitelist.json");
            getLogger().log(Level.SEVERE, "Stack trace: ", e);
        }

        return false;
    }

    private void handleLoginResponse(AsyncPlayerPreLoginEvent event, String playerName, HttpResponse<String> response) {
        JsonObject jsonResponse = JsonParser.parseString(response.body()).getAsJsonObject();
        String status = jsonResponse.has("status") ? jsonResponse.get("status").getAsString() : "failure";

        if (response.statusCode() == 200 && status.equals("success")) {
            getLogger().info(playerName + " was successfully authenticated!");
            // unnecessary, but for completeness sake, let's be explicit.
            event.allow();
        } else {
            getLogger().info("Couldn't authenticate " + playerName + ".");
            getLogger().info("StatusCode was: " + response.statusCode() + ", and body: " + jsonResponse.toString());
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST,
                    Component.text("Please register at " + Objects.requireNonNull(getConfig()).getString("register")));
        }
    }
}
