package it.tigierrei.combatlog;

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.gamemode.GameMode;
import org.spongepowered.api.entity.living.player.gamemode.GameModes;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.cause.entity.damage.source.EntityDamageSource;
import org.spongepowered.api.event.cause.entity.damage.source.IndirectEntityDamageSource;
import org.spongepowered.api.event.command.SendCommandEvent;
import org.spongepowered.api.event.entity.DamageEntityEvent;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.serializer.TextSerializers;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Plugin(
        id = "combatlog",
        name = "CombatLog",
        authors = "Tito Tigi"
)
public class CombatLog {

    @Inject
    private Logger logger;

    private final Map<UUID, Long> combatLog = new ConcurrentHashMap<>();
    private final Text enterMessage = TextSerializers.FORMATTING_CODE.deserialize("&4Sei entrato in CombatLog");
    private final Text exitMessage = TextSerializers.FORMATTING_CODE.deserialize("&6Sei uscito dal CombatLog");

    @Listener
    public void onServerStart(GameStartedServerEvent event) {
        Task.builder().execute(() -> {
            long currentTime = System.currentTimeMillis();
            Iterator<Map.Entry<UUID, Long>> iterator = combatLog.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<UUID, Long> entry = iterator.next();
                UUID uuid = entry.getKey();
                long time = entry.getValue();
                if (currentTime - time >= 15000) {
                    iterator.remove();
                    Sponge.getServer().getPlayer(uuid).ifPresent(player -> player.sendMessages(exitMessage));
                }
            }
        }).interval(1, TimeUnit.SECONDS).async().submit(this);
    }

    @Listener(order = Order.LAST, beforeModifications = true)
    public void onEntityDamaged(DamageEntityEvent event) {
        Object root = event.getCause().root();
        if (root instanceof EntityDamageSource && event.getTargetEntity() instanceof Player) {
            if (root instanceof IndirectEntityDamageSource) {
                IndirectEntityDamageSource src = (IndirectEntityDamageSource) root;
                Entity indirectSource = src.getIndirectSource();
                if (indirectSource instanceof Player) {
                    Player damager = (Player) indirectSource;
                    Player target = (Player) event.getTargetEntity();
                    Optional<GameMode> optional = damager.getGameModeData().get(Keys.GAME_MODE);
                    if(optional.isPresent()){
                        GameMode gameMode = optional.get();
                        if(gameMode.equals(GameModes.CREATIVE)){
                            return;
                        }
                    }
                    optional = target.getGameModeData().get(Keys.GAME_MODE);
                    if(optional.isPresent()){
                        GameMode gameMode = optional.get();
                        if(gameMode.equals(GameModes.CREATIVE)){
                            return;
                        }
                    }
                    long time = System.currentTimeMillis();
                    if(!combatLog.containsKey(damager.getUniqueId())){
                        damager.sendMessages(enterMessage);
                    }
                    if(!combatLog.containsKey(target.getUniqueId())){
                        target.sendMessages(enterMessage);
                    }
                    combatLog.put(damager.getUniqueId(), time);
                    combatLog.put(target.getUniqueId(), time);
                }
            } else {
                EntityDamageSource src = (EntityDamageSource) root;
                if (src.getSource() instanceof Player) {
                    Player damager = (Player) src.getSource();
                    Player target = (Player) event.getTargetEntity();
                    Optional<GameMode> optional = damager.getGameModeData().get(Keys.GAME_MODE);
                    if(optional.isPresent()){
                        GameMode gameMode = optional.get();
                        if(gameMode.equals(GameModes.CREATIVE)){
                            return;
                        }
                    }
                    optional = target.getGameModeData().get(Keys.GAME_MODE);
                    if(optional.isPresent()){
                        GameMode gameMode = optional.get();
                        if(gameMode.equals(GameModes.CREATIVE)){
                            return;
                        }
                    }
                    long time = System.currentTimeMillis();
                    if(!combatLog.containsKey(damager.getUniqueId())){
                        damager.sendMessages(enterMessage);
                    }
                    if(!combatLog.containsKey(target.getUniqueId())){
                        target.sendMessages(enterMessage);
                    }
                    combatLog.put(damager.getUniqueId(), time);
                    combatLog.put(target.getUniqueId(), time);
                }
            }
        }
    }

    @Listener
    public void onPlayerLeft(ClientConnectionEvent.Disconnect event){
        //logger.info(event.getCause().root().getClass().getName());
        Player player = event.getTargetEntity();
        UUID uuid = player.getUniqueId();
        if(combatLog.containsKey(uuid)){
            player.offer(player.getHealthData().set(Keys.HEALTH, 0D));
            combatLog.remove(uuid);
        }
    }

    @Listener
    public void onCommandExecute(SendCommandEvent event){
        String command = event.getCommand();
        String arguments = event.getArguments();
        if(event.getSource() instanceof Player){
            Player player = (Player) event.getSource();
            if(combatLog.containsKey(player.getUniqueId()) && (event.getCommand().equalsIgnoreCase("t") || command.equalsIgnoreCase("town")) && arguments.equalsIgnoreCase("spawn")){
                event.setCancelled(true);
            }
        }
    }
}
