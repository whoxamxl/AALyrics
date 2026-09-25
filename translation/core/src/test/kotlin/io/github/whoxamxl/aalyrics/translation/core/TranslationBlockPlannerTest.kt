package io.github.whoxamxl.aalyrics.translation.core

import io.github.whoxamxl.aalyrics.core.model.LyricsDocument
import io.github.whoxamxl.aalyrics.core.model.PlainLyricLine
import io.github.whoxamxl.aalyrics.core.model.TimedLyricLine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TranslationBlockPlannerTest {
    @Test
    fun `language blank and timestamp gaps form hard boundaries`() {
        val lyrics = LyricsDocument(
            lines = listOf(
                TimedLyricLine("English alpha", startMs = 0L, endMs = 1_000L),
                TimedLyricLine("English beta", startMs = 1_500L, endMs = 2_500L),
                PlainLyricLine(""),
                TimedLyricLine("English gamma", startMs = 3_000L, endMs = 4_000L),
                TimedLyricLine("English delta", startMs = 20_000L, endMs = 21_000L),
                TimedLyricLine("한국어 구절 하나", startMs = 21_500L, endMs = 22_500L),
            ),
        )
        val profile = profile(
            languages = listOf("en", "en", null, "en", "en", "ko"),
            roles = listOf(
                ProfiledLineRole.PRIMARY,
                ProfiledLineRole.PRIMARY,
                ProfiledLineRole.UNCERTAIN,
                ProfiledLineRole.PRIMARY,
                ProfiledLineRole.PRIMARY,
                ProfiledLineRole.SECONDARY,
            ),
            secondary = "ko",
            activation = SecondaryActivation.ACTIVE,
        )
        val planner = TranslationBlockPlanner(
            TranslationBlockPolicy(preferredCoreLines = 10, contextHaloLines = 1),
        )

        val plan = planner.plan(lyrics, profile, targetLanguage = "ja")

        assertEquals(
            listOf(listOf(0, 1), listOf(3), listOf(4), listOf(5)),
            plan.blocks.map { it.coreLineIndices },
        )
        assertTrue(plan.blocks.all { it.contextLineIndices.isEmpty() })
        assertEquals(TranslationLineDisposition.PRESERVE, plan.lines[2].disposition)
    }

    @Test
    fun `unsupported detected source language is preserved instead of routed`() {
        val lyrics = LyricsDocument(
            lines = listOf(
                PlainLyricLine("Supported primary line"),
                PlainLyricLine("Unsupported active secondary line"),
                PlainLyricLine("Supported primary line two"),
            ),
        )
        val profile = LanguageProfile(
            primary = "en",
            secondaryCandidate = "ar",
            secondaryActivation = SecondaryActivation.ACTIVE,
            lines = listOf(
                ProfiledLyricLine(
                    index = 0,
                    languageTag = "en",
                    confidence = 0.98f,
                    role = ProfiledLineRole.PRIMARY,
                ),
                ProfiledLyricLine(
                    index = 1,
                    languageTag = "ar",
                    confidence = 0.95f,
                    role = ProfiledLineRole.SECONDARY,
                ),
                ProfiledLyricLine(
                    index = 2,
                    languageTag = "en",
                    confidence = 0.98f,
                    role = ProfiledLineRole.PRIMARY,
                ),
            ),
        )

        val plan = TranslationBlockPlanner(
            TranslationBlockPolicy(contextHaloLines = 0),
        ).plan(
            lyrics = lyrics,
            profile = profile,
            targetLanguage = "ja",
        )

        assertEquals(
            listOf(
                TranslationLineDisposition.TRANSLATE,
                TranslationLineDisposition.PRESERVE,
                TranslationLineDisposition.TRANSLATE,
            ),
            plan.lines.map { it.disposition },
        )
        assertEquals(
            listOf(listOf(0), listOf(2)),
            plan.blocks.map { it.coreLineIndices },
        )
    }

    @Test
    fun `unsupported primary document produces no translation blocks`() {
        val lyrics = LyricsDocument(
            lines = listOf(
                PlainLyricLine("Unsupported primary one"),
                PlainLyricLine("Unsupported primary two"),
            ),
        )
        val profile = LanguageProfile(
            primary = "ar",
            secondaryCandidate = null,
            secondaryActivation = SecondaryActivation.NONE,
            lines = listOf(
                ProfiledLyricLine(0, "ar", 0.98f, ProfiledLineRole.PRIMARY),
                ProfiledLyricLine(1, "ar", 0.98f, ProfiledLineRole.PRIMARY),
            ),
        )

        val plan = TranslationBlockPlanner().plan(
            lyrics = lyrics,
            profile = profile,
            targetLanguage = "ja",
        )

        assertTrue(plan.blocks.isEmpty())
        assertTrue(
            plan.lines.all { it.disposition == TranslationLineDisposition.PRESERVE },
        )
    }

    @Test
    fun `soft splits overlap Context Halo without duplicate Core ownership`() {
        val lyrics = LyricsDocument(
            lines = List(7) { index -> PlainLyricLine("Synthetic English line $index") },
        )
        val profile = profile(
            languages = List(7) { "en" },
            roles = List(7) { ProfiledLineRole.PRIMARY },
        )
        val planner = TranslationBlockPlanner(
            TranslationBlockPolicy(
                preferredCoreLines = 3,
                maximumCoreCharacters = 1_000,
                contextHaloLines = 1,
            ),
        )

        val plan = planner.plan(lyrics, profile, targetLanguage = "ja")

        assertEquals(
            listOf(listOf(0, 1, 2), listOf(3, 4, 5), listOf(6)),
            plan.blocks.map { it.coreLineIndices },
        )
        assertEquals(listOf(3), plan.blocks[0].contextLineIndices)
        assertEquals(listOf(2, 6), plan.blocks[1].contextLineIndices)
        assertEquals(listOf(5), plan.blocks[2].contextLineIndices)
        val coreOwners = plan.blocks.flatMap { it.coreLineIndices }
        assertEquals((0..6).toList(), coreOwners.sorted())
        assertEquals(coreOwners.size, coreOwners.distinct().size)
        assertTrue(plan.blocks[0].contextLineIndices.single() in plan.blocks[1].coreLineIndices)
    }

    @Test
    fun `character limit is a soft boundary`() {
        val lyrics = LyricsDocument(
            lines = listOf(
                PlainLyricLine("abcdefgh"),
                PlainLyricLine("ijklmnop"),
                PlainLyricLine("qrstuvwx"),
            ),
        )
        val profile = profile(
            languages = List(3) { "en" },
            roles = List(3) { ProfiledLineRole.PRIMARY },
        )
        val planner = TranslationBlockPlanner(
            TranslationBlockPolicy(
                preferredCoreLines = 10,
                maximumCoreCharacters = 12,
                contextHaloLines = 0,
            ),
        )

        val plan = planner.plan(lyrics, profile, targetLanguage = "fr")

        assertEquals(listOf(listOf(0), listOf(1), listOf(2)), plan.blocks.map { it.coreLineIndices })
    }

    private fun profile(
        languages: List<String?>,
        roles: List<ProfiledLineRole>,
        secondary: String? = null,
        activation: SecondaryActivation = SecondaryActivation.NONE,
    ): LanguageProfile = LanguageProfile(
        primary = "en",
        secondaryCandidate = secondary,
        secondaryActivation = activation,
        lines = languages.indices.map { index ->
            ProfiledLyricLine(
                index = index,
                languageTag = languages[index],
                confidence = if (languages[index] == null) 0f else 0.95f,
                role = roles[index],
            )
        },
    )
}
