package com.are;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;

/** The book you receive when you first enter a world. Everything in it stays on your PC. */
public final class AreBook {
    private AreBook() {}

    private static MutableComponent hl(String s) {
        return Component.literal(s).withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD);
    }

    private static MutableComponent t(String s) {
        return Component.literal(s).withStyle(ChatFormatting.BLACK);
    }

    public static String systemUser() {
        return System.getProperty("user.name", "someone");
    }

    private static String hostName() {
        String h = System.getenv("COMPUTERNAME");
        if (h == null || h.isBlank()) h = System.getenv("HOSTNAME");
        if (h == null || h.isBlank()) {
            try {
                h = InetAddress.getLocalHost().getHostName();
            } catch (Exception e) {
                h = "this machine";
            }
        }
        return h;
    }

    public static ItemStack make(ServerPlayer p) {
        String name = p.getName().getString();
        boolean sys = AreConfig.SYSTEM_INFO.get();
        List<Filterable<Component>> pages = new ArrayList<>();

        pages.add(Filterable.passThrough(Component.empty()
                .append(t("Hello, ")).append(hl(name)).append(t(".\n\nYou made a new world.\n\nI was already here.\n\nI have been waiting for someone to open this book."))));

        if (sys) {
            String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm 'on' dd MMMM yyyy"));
            String os = System.getProperty("os.name", "your OS") + " " + System.getProperty("os.version", "");
            pages.add(Filterable.passThrough(Component.empty()
                    .append(t("I know you.\n\nNot the name you picked.\n\nThe name your machine calls you:\n\n"))
                    .append(hl(systemUser())).append(t("\n\nSitting at "))
                    .append(hl(hostName())).append(t("."))));
            pages.add(Filterable.passThrough(Component.empty()
                    .append(t("It is ")).append(hl(time)).append(t(".\n\nYour "))
                    .append(t(os.trim())).append(t(" is humming.\n\n"))
                    .append(t(Runtime.getRuntime().availableProcessors() + " cores. Java " + System.getProperty("java.version", "?")))
                    .append(t(".\n\nNobody is coming."))));
        } else {
            pages.add(Filterable.passThrough(Component.empty()
                    .append(t("I know the name you picked.\n\n")).append(hl(name)).append(t(".\n\nThat is enough."))));
        }

        pages.add(Filterable.passThrough(Component.empty()
                .append(hl("THE RULES\n\n"))
                .append(t("1. The Watcher only watches. Look at it and it leaves.\n\n"))
                .append(t("2. The Hollow only moves when you do not look.\n\n"))
                .append(t("3. Torches lie.\n\n4. Do not sleep. Sleep is where I live."))));

        pages.add(Filterable.passThrough(Component.empty()
                .append(t("Survive "))
                .append(hl("10 nights"))
                .append(t(".\n\nThen you may leave.\n\nOr you may stay.\n\nI will be fine either way.\n\n"))
                .append(hl("- A"))));

        WrittenBookContent content = new WrittenBookContent(Filterable.passThrough("Are"), "???", 0, pages, true);
        ItemStack stack = new ItemStack(Items.WRITTEN_BOOK);
        stack.set(DataComponents.WRITTEN_BOOK_CONTENT, content);
        return stack;
    }
}
