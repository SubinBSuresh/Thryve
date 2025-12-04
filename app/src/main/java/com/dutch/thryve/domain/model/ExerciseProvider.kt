package com.dutch.thryve.domain.model

object ExerciseProvider {
    val exercises = listOf(
        // Abs
//        Exercise(id = "plank", name = "Plank", category = "Abs"),
//        Exercise(id = "crunches", name = "Crunches", category = "Abs"),
//        Exercise(id = "leg_raises", name = "Leg Raises", category = "Abs"),
//        Exercise(id = "hanging_leg_raises", name = "Hanging Leg Raises", category = "Abs"),

        // Back
//        Exercise(id = "barbell_row", name = "Barbell Row", category = "Back"),
        Exercise(id = "cable_machine_row_close_grip", name = "Cable Machine Row Close Grip", category = "Back"),
        Exercise(id = "cable_machine_row_vbar", name = "Cable Machine Row V-Bar", category = "Back"),
//        Exercise(id = "chin_up", name = "Chin-up", category = "Back"),
        Exercise(id = "deadlift", name = "Deadlift", category = "Back"),
        Exercise(id = "face_pulls", name = "Face Pulls", category = "Back"),
        Exercise(id = "lat_pulldown_straight_bar", name = "Lat Pulldown Straight Bar", category = "Back"),
        Exercise(id = "lat_pulldown_vbar", name = "Lat Pulldown V-Bar", category = "Back"),
        Exercise(id = "linear_row", name = "Linear Row", category = "Back"),
//        Exercise(id = "pull_up", name = "Pull-up", category = "Back"),
        Exercise(id = "shrugs", name = "Shrugs", category = "Back"),

        // Bicep
        Exercise(id = "cable_bicep_curls", name = "Cable Bicep Curls", category = "Bicep"),
        Exercise(id = "concentration_curls", name = "Concentration Curls", category = "Bicep"),
        Exercise(id = "dumbbell_curl", name = "Dumbbell Curl", category = "Bicep"),
        Exercise(id = "hammer_curls", name = "Hammer Curls", category = "Bicep"),
        Exercise(id = "preacher_curls_machine", name = "Preacher Curls Machine", category = "Bicep"),
        Exercise(id = "preacher_curls_dumbbell", name = "Preacher Curls Dumbbell", category = "Bicep"),
        Exercise(id = "ez_barbell_curls", name = "EZ Barbell Curls", category = "Bicep"),

        // Chest
        Exercise(id = "cable_crossovers", name = "Cable Crossovers", category = "Chest"),
        Exercise(id = "cable_fly", name = "Cable Fly", category = "Chest"),
        Exercise(id = "dips", name = "Dips", category = "Chest"),
        Exercise(id = "dumbbell_chest_press", name = "Dumbbell Chest Press", category = "Chest"),
        Exercise(id = "flat_bench_press", name = "Flat Bench Press", category = "Chest"),
        Exercise(id = "inclined_bench_press", name = "Inclined Bench Press", category = "Chest"),
        Exercise(id = "peck_deck_machine", name = "Peck Deck Machine", category = "Chest"),

        // Forearms
        Exercise(id = "forearm_curls_dumbbell", name = "Forearm Curls Dumbbell", category = "Forearms"),
        Exercise(id = "forearm_curls_machine", name = "Forearm Curls Machine", category = "Forearms"),
        Exercise(id = "forearm_curl_bar", name = "Forearm Curl Bar", category = "Forearms"),

        // Legs
        Exercise(id = "barbell_squat", name = "Barbell Squat", category = "Legs"),
        Exercise(id = "calf_raise", name = "Calf Raise", category = "Legs"),
        Exercise(id = "goblet_squat", name = "Goblet Squat", category = "Legs"),
        Exercise(id = "leg_curl", name = "Leg Curl", category = "Legs"),
        Exercise(id = "leg_extension", name = "Leg Extension", category = "Legs"),
        Exercise(id = "leg_press", name = "Leg Press", category = "Legs"),
        Exercise(id = "lunges", name = "Lunges", category = "Legs"),

        // Shoulder
        Exercise(id = "arnold_press", name = "Arnold Press", category = "Shoulder"),
        Exercise(id = "barbell_upright_row", name = "Barbell Upright Row", category = "Shoulder"),
        Exercise(id = "cable_lateral_raise", name = "Cable Lateral Raise", category = "Shoulder"),
        Exercise(id = "dumbbell_front_raises", name = "Dumbbell Front Raises", category = "Shoulder"),
        Exercise(id = "dumbbell_lateral_raise", name = "Dumbbell Lateral Raise", category = "Shoulder"),
        Exercise(id = "dumbbell_shoulder_press", name = "Dumbbell Shoulder Press", category = "Shoulder"),
        Exercise(id = "overhead_press", name = "Overhead Press", category = "Shoulder"),

        // Tricep
        Exercise(id = "cable_single_arm_kickbacks", name = "Cable Single Arm Kickbacks", category = "Tricep"),
        Exercise(id = "dumbbell_tricep_extension", name = "Dumbbell Triceps Extension", category = "Tricep"),
        Exercise(id = "overhead_tricep_extension_bar", name = "Overhead Triceps Extension Bar", category = "Tricep"),
        Exercise(id = "overhead_tricep_extension_dumbbell", name = "Overhead Triceps Extension Dumbbell", category = "Tricep"),
        Exercise(id = "skull_crushers", name = "Skull Crushers", category = "Tricep"),
        Exercise(id = "tricep_pushdown_bar", name = "Triceps Pushdown Bar", category = "Tricep"),
        Exercise(id = "tricep_pushdown_rope", name = "Triceps Pushdown Rope", category = "Tricep")
    ).sortedBy { it.name }
}
