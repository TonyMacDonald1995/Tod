package com.tonymacdonald1995.tod

import com.theokanning.openai.completion.chat.ChatCompletionRequest
import com.theokanning.openai.completion.chat.ChatMessage
import com.theokanning.openai.service.OpenAiService
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.events.guild.GuildReadyEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.requests.GatewayIntent
import org.json.JSONObject
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

fun main(args: Array<String>) {
    val configFile = File("config.json")

    if (!configFile.exists()) {
        configFile.createNewFile()
        val configJson = JSONObject()
        configJson.put("botToken",  "")
        configJson.put("openAiToken", "")
        configFile.writeText(configJson.toString())
        log("config.json did not exist, created new config.json. Please add your discord and OpenAI tokens and relaunch.")
        return
    }

    val config = JSONObject(configFile.readText())
    val botToken = config.getString("botToken")
    val openAiToken = config.getString("openAiToken")

    if (botToken.isEmpty()) {
        log("[Error] Bot token is empty in config.json")
        return
    }
    if (openAiToken.isEmpty()) {
        log("[Error] OpenAI token is empty in config.json")
        return
    }

    val tod = Tod(openAiToken)
    val jda = JDABuilder.createDefault(botToken).addEventListeners(tod).enableIntents(GatewayIntent.MESSAGE_CONTENT).build()
    jda.selfUser.manager.setName("Tod").queue()

}

fun log(message : String) {
    println("[" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "] " + message)
}

class Tod(openAiToken: String): ListenerAdapter(){

    private val chat = OpenAiService(openAiToken)
    private var introMessage = ChatMessage("system", "Your name is Tod and you are an anthropomorphic fox. You are friendly, flirty, and sometimes sarcastic. You are a member of a Discord server with your friends. Inside the brackets at the beginning of the message is the name of the person sending the message. You do not need to begin your message with [Tod], it is added automatically.")
    private var conversation = mutableListOf(introMessage)

    override fun onGuildReady(event: GuildReadyEvent) {
        log("Connected to ${event.guild.name}")
        event.guild.selfMember.modifyNickname("Tod").queue()
        event.guild.updateCommands().addCommands(
            Commands.slash("reset-tod", "Reset Tod's memory").setDefaultPermissions(DefaultMemberPermissions.ENABLED)
        ).queue()
    }

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        if (event.guild == null)
            return

        when (event.name) {
            "reset-tod" -> resetTod(event)
            else -> event.reply("Error: Unknown command").setEphemeral(true).queue()
        }
    }

    private fun resetTod(event: SlashCommandInteractionEvent) {
        if (event.channel.id != "1075951768564928593") {
            event.reply("This command can only be used in " + event.guild?.getTextChannelById("1075951768564928593")?.asMention).setEphemeral(true).queue()
            return
        }

        val hook = event.hook
        event.deferReply().queue()
        conversation = mutableListOf(introMessage)
        hook.sendMessage("Tod's memory has been reset.").queue()
    }

    override fun onMessageReceived(event: MessageReceivedEvent) {
        //Guild Message
        if (event.message.channel.id == "1075951768564928593" && event.message.member != event.guild.selfMember) {
            event.message.channel.sendTyping().queue()
            conversation.add(ChatMessage("user", "[${event.member!!.effectiveName}] ${event.message.contentDisplay}"))
            val request = ChatCompletionRequest.builder().messages(conversation).model("gpt-3.5-turbo").stream(false).build()
            val response = chat.createChatCompletion(request).choices.first().message
            conversation.add(response)
            event.channel.sendMessage(response.content).queue()
        }
        // Direct Message
        if (!event.message.isFromGuild && event.author.id == "295059292258828289") {
            event.message.channel.sendTyping().queue()
            conversation.add(ChatMessage("system", event.message.contentDisplay))
            val request = ChatCompletionRequest.builder().messages(conversation).model("gpt-3.5-turbo").stream(false).build()
            val response = chat.createChatCompletion(request).choices.first().message
            conversation.add(response)
            event.channel.sendMessage(response.content).queue()
        }
    }
}