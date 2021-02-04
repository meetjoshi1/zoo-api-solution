package com.galvanize.zoo.animal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.galvanize.zoo.habitat.HabitatEntity;
import com.galvanize.zoo.habitat.HabitatRepository;
import com.galvanize.zoo.habitat.HabitatType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import javax.transaction.Transactional;
import java.util.List;

import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;


@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@AutoConfigureRestDocs(outputDir = "target/snippets")
class AnimalControllerIT {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    AnimalRepository animalRepository;

    @Autowired
    HabitatRepository habitatRepository;

    @Test
    void create_fetchAll() throws Exception {
        AnimalDto input = new AnimalDto("monkey", AnimalType.WALKING, null, null);
        mockMvc.perform(
            post("/animals")
                .content(objectMapper.writeValueAsString(input))
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isCreated());

        mockMvc.perform(get("/animals"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("length()").value(1))
            .andExpect(jsonPath("[0].name").value("monkey"))
            .andExpect(jsonPath("[0].mood").value(Mood.UNHAPPY.name()))
            .andExpect(jsonPath("[0].type").value(AnimalType.WALKING.name()))
                .andDo(document("animal", responseFields(
                        fieldWithPath("[0].name").description("Name of the animal"),
                        fieldWithPath("[0].mood").description("Mood of the animal"),
                        fieldWithPath("[0].type").description("Type of the animal"),
                        fieldWithPath("[0].habitat").description("habitat of the animal")
                )));
    }

    @Test
    void create_conflict() throws Exception {
        animalRepository.save(new AnimalEntity("monkey", AnimalType.WALKING));

        AnimalDto input = new AnimalDto("monkey", AnimalType.WALKING, null, null);
        mockMvc.perform(
            post("/animals")
                .content(objectMapper.writeValueAsString(input))
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isConflict());
    }

    @Test
    void feed() throws Exception {
        animalRepository.save(new AnimalEntity("monkey", AnimalType.WALKING));

        mockMvc.perform(post("/animals/monkey/feed"))
            .andExpect(status().isOk());

        mockMvc.perform(get("/animals"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("[0].name").value("monkey"))
            .andExpect(jsonPath("[0].mood").value(Mood.HAPPY.name()));
    }

    @Test
    void move() throws Exception {
        animalRepository.save(new AnimalEntity("monkey", AnimalType.WALKING));
        habitatRepository.save(new HabitatEntity("Monkey's Jungle", HabitatType.FOREST));

        mockMvc.perform(post("/animals/monkey/move").content("Monkey's Jungle"))
            .andExpect(status().isOk());

        mockMvc.perform(get("/animals"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("[0].name").value("monkey"))
            .andExpect(jsonPath("[0].habitat.name").value("Monkey's Jungle"));
    }

    @Test
    void move_incompatible() throws Exception {
        AnimalEntity eagle = new AnimalEntity("eagle", AnimalType.FLYING);
        eagle.setMood(Mood.HAPPY);
        animalRepository.save(eagle);
        habitatRepository.save(new HabitatEntity("Monkey's Jungle", HabitatType.FOREST));

        mockMvc.perform(post("/animals/eagle/move").content("Monkey's Jungle"))
            .andExpect(status().isConflict());

        mockMvc.perform(get("/animals"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("[0].name").value("eagle"))
            .andExpect(jsonPath("[0].mood").value(Mood.UNHAPPY.name()))
            .andExpect(jsonPath("[0].habitat").isEmpty());
    }

    @Test
    void move_occupied() throws Exception {
        animalRepository.save(new AnimalEntity("monkey", AnimalType.WALKING));

        HabitatEntity habitat = new HabitatEntity("Monkey's Jungle", HabitatType.FOREST);
        AnimalEntity chimp = new AnimalEntity("chimp", AnimalType.WALKING);
        chimp.setHabitat(habitat);
        habitat.setAnimal(chimp);
        habitatRepository.save(habitat);

        mockMvc.perform(post("/animals/monkey/move").content("Monkey's Jungle"))
            .andExpect(status().isConflict());

        mockMvc.perform(get("/animals"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("[0].name").value("monkey"))
            .andExpect(jsonPath("[0].habitat").isEmpty())
            .andExpect(jsonPath("[1].name").value("chimp"))
            .andExpect(jsonPath("[1].habitat.name").value("Monkey's Jungle"));
    }

    @Test
    void fetch_withParams() throws Exception {
        AnimalEntity monkey = new AnimalEntity("monkey", AnimalType.WALKING);
        AnimalEntity eagle = new AnimalEntity("eagle", AnimalType.FLYING);
        AnimalEntity whale = new AnimalEntity("whale", AnimalType.SWIMMING);
        AnimalEntity chimp = new AnimalEntity("chimp", AnimalType.WALKING);
        chimp.setMood(Mood.HAPPY);
        animalRepository.saveAll(List.of(monkey, eagle, whale, chimp));

        mockMvc.perform(get("/animals?mood=HAPPY&type=WALKING"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("length()").value(1))
            .andExpect(jsonPath("[0].name").value("chimp"))
            .andExpect(jsonPath("[0].mood").value(Mood.HAPPY.name()))
            .andExpect(jsonPath("[0].type").value(AnimalType.WALKING.name()));
    }
}
