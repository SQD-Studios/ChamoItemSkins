package net.chamosmp.chamoitemskins.api.service;

import net.chamosmp.chamoitemskins.api.objects.SkinGrant;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.UUID;


@ApiStatus.Internal
public interface CacheService {

    void putGrants(@NotNull UUID playerUuid, @NotNull Collection<SkinGrant> grants);

    Collection<SkinGrant> getGrants(@NotNull UUID playerUuid);

    void invalidate(@NotNull UUID playerUuid);
}
