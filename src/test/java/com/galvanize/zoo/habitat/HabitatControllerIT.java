package com.galvanize.zoo.habitat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.galvanize.zoo.animal.AnimalEntity;
import com.galvanize.zoo.animal.AnimalType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import javax.transaction.Transactional;

import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@AutoConfigureRestDocs(outputDir = "target/snippets")
class HabitatControllerIT {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    HabitatRepository habitatRepository;

    @Test
    void create_fetchAll() throws Exception {
        HabitatDto input = new HabitatDto("Eagle exhibit", HabitatType.NEST);
        mockMvc.perform(
            post("/habitats")
                .content(objectMapper.writeValueAsString(input))
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isCreated());

        mockMvc.perform(get("/habitats"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("length()").value(1))
            .andExpect(jsonPath("[0].name").value("Eagle exhibit"))
            .andExpect(jsonPath("[0].type").value(HabitatType.NEST.name()))
                .andDo(document("habitats", responseFields(
                        fieldWithPath("[0].name").description("Name of the habitat"),
                        fieldWithPath("[0].type").description("type of the habitat")
                )));

    }

    @Test
    void fetch_emptyHabitats() throws Exception {
        HabitatEntity habitat = new HabitatEntity("Monkey's Jungle", HabitatType.FOREST);
        AnimalEntity chimp = new AnimalEntity("chimp", AnimalType.WALKING);
        chimp.setHabitat(habitat);
        habitat.setAnimal(chimp);
        habitatRepository.save(habitat);

        habitatRepository.save(new HabitatEntity("Eagle's Nest", HabitatType.NEST));

        mockMvc.perform(get("/habitats?onlyShowEmpty=true"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("length()").value(1))
            .andExpect(jsonPath("[0].name").value("Eagle's Nest"))
            .andExpect(jsonPath("[0].type").value(HabitatType.NEST.name()));
    }
}
