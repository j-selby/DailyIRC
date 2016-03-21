package net.jselby.dirc

val escapeCharacter = 3.toChar()

fun escapeJavascript(code : String): String {
    return code.replace("\t", "\\t").replace("\\", "\\\\").replace("\"", "\\\"")
}

fun escapeHTML(code : String): String {
    return code.replace("<", "&lt;").replace(">", "&gt;")
}

fun isInteger(string: String) : Boolean {
    try {
        string.toInt()
        return true
    } catch (e : NumberFormatException) {
        return false
    }
}

fun ircMsgToHTML(message : String) : String {
    var cleanInput = escapeHTML(message)

    if (cleanInput.startsWith(" ")) {
        cleanInput = "&nbsp;" + cleanInput.substring(1)
    }

    if (cleanInput.endsWith(" ")) {
        cleanInput = cleanInput.substring(0, cleanInput.length - 1) + "&nbsp;"
    }

    // Iterate over colors
    while(cleanInput.contains(escapeCharacter)) {
        val index = cleanInput.indexOf(escapeCharacter)
        val section = cleanInput.substring(index + 1,
                index + if (cleanInput.indexOf(',', index) == index + 3) 6 else 3) // Foreground:Background : Foreground
        val args = section.split(",")
        var replacement = ""

        if (args.size == 2 && isInteger(args[0]) && isInteger(args[1])) {
            replacement = "</span><span style=\"color: " + Colors.values()
                    .filter({it.rawColor == args[0].toInt()})
                    .single().name.toLowerCase() + "; background: " +
                    Colors.values().filter({it.rawColor == args[1].toInt()})
                    .single().name.toLowerCase() + "\">"
        } else if (args.size == 1 && isInteger(args[0])) {
            replacement = "</span><span style=\"color: " + Colors.values()
                    .filter({it.rawColor == args[0].toInt()})
                    .single().name.toLowerCase() + "\">"
        }
        cleanInput = cleanInput.replaceFirst("$escapeCharacter$section", replacement)
    }

    return cleanInput.replace("> ", ">&nbsp;").replace(" <", "&nbsp;<")
}

enum class Colors(val rawColor: kotlin.Int) {
    WHITE(0),
    BLACK(1),
    BLUE(2),
    GREEN(3),
    RED(4),
    BROWN(5),
    PURPLE(6),
    ORANGE(7),
    YELLOW(8),
    LIGHTGREEN(9),
    TEAL(10),
    LIGHTCYAN(11),
    LIGHTBLUE(12),
    PINK(13),
    GREY(14),
    LIGHTGREY(15),
    DEFAULT(99)
}