/**
 * The public What Hit Me? API for script mods and other mods (Java 8, no Minecraft classes).
 *
 * <p>Everything goes through {@link io.github.junjun1z.whathitme.api.WhatHitMeApi}. The API is server side: conditions,
 * variables and listeners run on the logical server (also the integrated server of single player). Script exceptions are
 * caught, logged once per function and count as false / empty.
 *
 * <p>Registrations belong to an owner (for example "kubejs" or "crafttweaker"). Registrations made inside a register hook
 * ({@link io.github.junjun1z.whathitme.api.WhatHitMeApi#onRegister}) are removed and made again on every rebuild
 * (server start, {@code /whm reload}, the script mod's reload). Registrations made outside a hook stay until
 * {@link io.github.junjun1z.whathitme.api.WhatHitMeApi#clear} is called for their owner (the script bindings do that when
 * their scripts reload). Registering a name again replaces the old entry, so there are no duplicates.
 */
package io.github.junjun1z.whathitme.api;
