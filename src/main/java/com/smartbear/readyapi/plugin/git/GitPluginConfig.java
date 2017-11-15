package com.smartbear.readyapi.plugin.git;

import com.eviware.soapui.plugins.PluginAdapter;
import com.eviware.soapui.plugins.PluginConfiguration;

@PluginConfiguration(groupId = "com.smartbear.readyapi.plugins", name = "Git Integration Plugin",
        version = "1.1.0-SNAPSHOT", autoDetect = true,
        description = "A git plugin to share composite projects", infoUrl = "https://github.com/SmartBear/ready-git-plugin",
        minimumReadyApiVersion = "2.3.0")
public class GitPluginConfig extends PluginAdapter {

}
