package org.libreproject.bramble;

import org.libreproject.bramble.network.JavaNetworkModule;
import org.libreproject.bramble.plugin.tor.CircumventionModule;
import org.libreproject.bramble.socks.SocksModule;
import org.libreproject.bramble.system.JavaSystemModule;

import dagger.Module;

@Module(includes = {
		CircumventionModule.class,
		JavaNetworkModule.class,
		JavaSystemModule.class,
		SocksModule.class
})
public class BrambleJavaModule {

}
