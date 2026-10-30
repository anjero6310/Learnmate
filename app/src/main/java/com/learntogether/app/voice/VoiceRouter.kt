package com.learntogether.app.voice

import androidx.navigation.NavHostController
import com.learntogether.app.data.AppViewModel
import com.learntogether.app.data.Subject
import com.learntogether.app.ui.Routes
import com.learntogether.app.ui.navigateToTab

/** Carries out voice commands: navigation, lesson control, settings, and spoken answers. */
class VoiceRouter(
    private val vm: AppViewModel,
    private val nav: NavHostController
) {

    /** Tries each recognition alternative until one is a command we know. */
    fun handle(heard: List<String>) {
        val command = heard.firstNotNullOfOrNull { VoiceParser.parse(it, vm.subjects) }
        if (command == null) {
            if (vm.isFilipinoVoice) {
                vm.announceIn("Pasensya na, hindi ko naintindihan. Sabihin ang tulong.", "fil-PH")
            } else {
                vm.announce("Sorry, I didn't understand. Say help to hear what you can say.")
            }
            return
        }
        run(command)
    }

    private fun currentRoute(): String? = nav.currentDestination?.route
    private fun currentArg(): String? = nav.currentBackStackEntry?.arguments?.getString("id")

    private fun run(command: VoiceCommand) {
        when (command) {
            VoiceCommand.Stop -> vm.stopSpeaking()
            VoiceCommand.Help -> vm.announceHelp()
            VoiceCommand.WhereAmI -> vm.announce(describeCurrent())
            VoiceCommand.Back -> goBack()
            VoiceCommand.Remaining -> vm.announce(remaining())
            VoiceCommand.ListLessons -> vm.announce(listLessons())

            is VoiceCommand.GoTab -> {
                val route = when (command.tab) {
                    VoiceTab.Home -> Routes.HOME
                    VoiceTab.Lessons -> Routes.LESSONS
                    VoiceTab.Progress -> Routes.PROGRESS
                    VoiceTab.Settings -> Routes.SETTINGS
                }
                nav.navigateToTab(route)
                vm.announce(describe(route, null))
            }
            is VoiceCommand.OpenSubject -> {
                leaveLessonIfOpen()
                nav.navigate(Routes.subject(command.subject.id)) { launchSingleTop = true }
                vm.announce(describeSubject(command.subject))
            }
            is VoiceCommand.OpenLesson -> openLessonByVoice(command.lesson.id)

            is VoiceCommand.ContrastMode -> {
                vm.updateHighContrast(command.on)
                vm.announce(if (command.on) "High contrast is on." else "High contrast is off.")
            }
            is VoiceCommand.TextSize -> {
                val before = vm.textScale
                val name = vm.stepTextScale(if (command.bigger) 1 else -1)
                vm.announce(
                    if (before == vm.textScale) "Text size is already ${name.lowercase()}."
                    else "Text size is now ${name.lowercase()}."
                )
            }
            is VoiceCommand.Speed -> {
                val target = (Math.round((vm.speechRate + if (command.faster) 0.2f else -0.2f) * 10) / 10f)
                    .coerceIn(0.5f, 1.5f)
                if (target == vm.speechRate) {
                    vm.announce(if (command.faster) "That is the fastest speed." else "That is the slowest speed.")
                } else {
                    vm.updateSpeechRate(target)
                    vm.announce(if (command.faster) "Faster." else "Slower.")
                }
            }

            VoiceCommand.Next, VoiceCommand.Previous, VoiceCommand.Repeat, VoiceCommand.Complete ->
                runLessonCommand(command)
        }
    }

    /** Opening something from inside a lesson replaces that lesson instead of stacking on it. */
    private fun leaveLessonIfOpen() {
        if (currentRoute() == Routes.LESSON) nav.popBackStack()
    }

    private fun openLessonByVoice(lessonId: String) {
        leaveLessonIfOpen()
        vm.openLesson(lessonId, readAloud = true)
        nav.navigate(Routes.lesson(lessonId)) { launchSingleTop = true }
    }

    private fun runLessonCommand(command: VoiceCommand) {
        val lesson = if (currentRoute() == Routes.LESSON) currentArg()?.let(vm::lesson) else null
        if (lesson == null) {
            // "Next" or "continue" outside a lesson starts the next lesson that isn't finished.
            if (command == VoiceCommand.Next) {
                val next = vm.allLessons.firstOrNull { !vm.isCompleted(it.id) }
                if (next != null) {
                    openLessonByVoice(next.id)
                } else {
                    vm.announce("You finished every lesson. Great job!")
                }
                return
            }
            vm.announce("Open a lesson first. Say open, then the name of a lesson, or say continue.")
            return
        }
        val page = vm.lessonPage.coerceIn(0, lesson.cards.lastIndex)
        val last = lesson.cards.lastIndex
        when (command) {
            VoiceCommand.Next -> when {
                page < last -> vm.goToCard(page + 1, readAloud = true)
                vm.isCompleted(lesson.id) -> vm.announce("This is the last card. You already finished this lesson.")
                else -> vm.announce("This is the last card. Say complete to finish the lesson.")
            }
            VoiceCommand.Previous ->
                if (page > 0) vm.goToCard(page - 1, readAloud = true)
                else vm.announce("This is the first card.")
            VoiceCommand.Repeat -> {
                val subject = vm.subject(lesson.subjectId) ?: return
                vm.speakCard(subject, lesson.cards[page])
            }
            VoiceCommand.Complete -> when {
                vm.isCompleted(lesson.id) -> vm.announce("You already finished this lesson.")
                page < last -> vm.announce("There are ${last - page} more cards. Say next to keep going.")
                else -> {
                    vm.markComplete(lesson.id)
                    vm.announce("Lesson complete! Great job. Say back to return, or say open and another lesson name.")
                }
            }
            else -> Unit
        }
    }

    private fun goBack() {
        val route = currentRoute()
        if (route == Routes.LESSON || route == Routes.SUBJECT) {
            nav.popBackStack()
            vm.announce("Going back.")
        } else {
            vm.announce("You are already on a main screen. Say home, lessons, progress or settings.")
        }
    }

    // ---- Spoken descriptions ------------------------------------------------

    private fun describeCurrent(): String = describe(currentRoute(), currentArg())

    private fun describe(route: String?, id: String?): String = when (route) {
        Routes.HOME -> {
            val names = vm.subjects.joinToString(", ") { it.title }
            "Home. Your subjects are $names. Say open, then a subject name."
        }
        Routes.LESSONS -> "All lessons. ${titles(vm.allLessons.map { it.title })} Say open, then a lesson name."
        Routes.SUBJECT -> vm.subject(id.orEmpty())?.let(::describeSubject) ?: "Subject."
        Routes.LESSON -> vm.lesson(id.orEmpty())?.let {
            "${it.title}. Card ${vm.lessonPage.coerceIn(0, it.cards.lastIndex) + 1} of ${it.cards.size}."
        } ?: "Lesson."
        Routes.PROGRESS ->
            "Progress. You finished ${vm.completedCount} of ${vm.totalLessons} lessons. " +
                "${vm.badges} badges earned. Day streak ${vm.streak}."
        Routes.SETTINGS ->
            "Settings. High contrast is ${if (vm.highContrast) "on" else "off"}. " +
                "Read aloud is ${if (vm.autoRead) "on" else "off"}. " +
                "Audio speed is ${"%.1f".format(vm.speechRate)}."
        else -> "Learnmate."
    }

    private fun describeSubject(subject: Subject) =
        "${subject.title}. ${titles(subject.lessons.map { it.title })} Say open, then a lesson name."

    private fun titles(list: List<String>): String =
        "${list.size} ${if (list.size == 1) "lesson" else "lessons"}: ${list.joinToString(", ")}."

    private fun remaining(): String {
        val left = vm.allLessons.filter { !vm.isCompleted(it.id) }
        if (left.isEmpty()) return "You finished every lesson. Great job!"
        val names = left.take(5).joinToString(", ") { it.title }
        val more = if (left.size > 5) " and ${left.size - 5} more" else ""
        return "You have ${left.size} ${if (left.size == 1) "lesson" else "lessons"} left: $names$more."
    }

    private fun listLessons(): String {
        val subject = if (currentRoute() == Routes.SUBJECT) vm.subject(currentArg().orEmpty()) else null
        val lessons = subject?.lessons ?: vm.allLessons
        return titles(lessons.map { it.title }) + " Say open, then a lesson name."
    }

    companion object {
        const val HELP_FIL =
            "Maaari mong sabihin: buksan, at ang pangalan ng aralin. Susunod. Nakaraan. Ulitin. " +
                "Tapos na. Bagalan. Bilisan. Bumalik. Tahanan. Mga aralin. Pag-unlad. Setting. " +
                "Nasaan ako. Ano ang natitira. Lakihan ang letra. Liitan ang letra. Kontras. O tigil."

        const val HELP =
            "You can say: open, then a subject or lesson name. Continue, to start your next lesson. " +
                "In a lesson: next. Previous. Repeat. Complete. " +
                "Slower. Faster. Go back. Home. Lessons. Progress. Settings. Where am I. " +
                "What is left. High contrast on or off. Bigger text. Smaller text. Or stop."
    }
}
