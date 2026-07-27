package net.chamosmp.chamoitemskins.manager;

import net.chamosmp.chamoitemskins.api.objects.SkinGrant;
import net.chamosmp.chamoitemskins.api.service.CacheService;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Manages in-memory cache for player skin grants.
 */
public final class CacheManager implements CacheService {
    private final Map<UUID, CacheEntry<Collection<SkinGrant>>> grantCache = new ConcurrentHashMap<>();
    private final long ttlMillis;

    public CacheManager(long ttlSeconds) {
        this.ttlMillis = TimeUnit.SECONDS.toMillis(ttlSeconds);
    }

    @Override
    public void putGrants(@NotNull UUID playerUuid, @NotNull Collection<SkinGrant> grants) {
        grantCache.put(playerUuid, new CacheEntry<>(grants, System.currentTimeMillis() + ttlMillis));
    }

    @Override
    public Collection<SkinGrant> getGrants(@NotNull UUID playerUuid) {
        CacheEntry<Collection<SkinGrant>> entry = grantCache.get(playerUuid);
        if (entry == null || System.currentTimeMillis() > entry.expiry()) {
            grantCache.remove(playerUuid);
            return null;
        }
        return entry.data();
    }

    @Override
    public void invalidate(@NotNull UUID playerUuid) {
        grantCache.remove(playerUuid);
    }

    private record CacheEntry<T>(T data, long expiry) {
    }
}