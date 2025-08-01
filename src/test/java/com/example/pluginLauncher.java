package com.example;

import com.TagPlayersPlugin.TagPlayersPlugin;
import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class pluginLauncher
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(TagPlayersPlugin.class);
		RuneLite.main(args);
	}
}