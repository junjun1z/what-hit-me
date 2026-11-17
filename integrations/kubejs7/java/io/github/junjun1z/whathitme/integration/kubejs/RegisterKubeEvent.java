package io.github.junjun1z.whathitme.integration.kubejs;

import dev.latvian.mods.kubejs.event.KubeEvent;

/**
 * WhatHitMeEvents.register: everything registered here is removed and registered again on every rebuild (server start,
 * /whm reload, /reload).
 */
public class RegisterKubeEvent extends WhatHitMeJS implements KubeEvent {
}
