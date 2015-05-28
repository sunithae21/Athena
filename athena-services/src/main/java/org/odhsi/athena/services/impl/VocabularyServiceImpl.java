package org.odhsi.athena.services.impl;

import org.odhsi.athena.dao.VocabularyDAO;
import org.odhsi.athena.dto.SimpleStatusDTO;
import org.odhsi.athena.dto.VocabularyStatusDTO;
import org.odhsi.athena.entity.Vocabulary;
import org.odhsi.athena.services.VocabularyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by GMalikov on 14.05.2015.
 */
public class VocabularyServiceImpl implements VocabularyService {
    @Autowired
    private VocabularyDAO vocabularyDAO;

    private DocumentBuilderFactory factory;

    private static final Logger LOGGER = LoggerFactory.getLogger(VocabularyServiceImpl.class);

    @Override
    public Vocabulary getById(String id) {
        return vocabularyDAO.getVocabularyById(id);
    }

    @Override
    public List<Vocabulary> getAllVocabularies() {
        return vocabularyDAO.getAllVocabularies();
    }

    @Override
    public List<VocabularyStatusDTO> getAllVocabularyStatuses() {
        List<Vocabulary> vocabularies = getAllVocabularies();
        List<VocabularyStatusDTO> result = new ArrayList<>();
        for (Vocabulary current : vocabularies) {
            result.add(makeDTOWithCurrentStatus(current));
        }
        return result;
    }

    @Override
    public List<SimpleStatusDTO> getSimpleStatuses() {
        List<Vocabulary> vocabularies = getAllVocabularies();
        List<SimpleStatusDTO> result = new ArrayList<>();
        for (Vocabulary current : vocabularies) {
            result.add(new SimpleStatusDTO(current));
        }
        return result;
    }

    private VocabularyStatusDTO makeDTOWithCurrentStatus(Vocabulary vocabulary) {
        VocabularyStatusDTO dto = new VocabularyStatusDTO(vocabulary);
        if (this.factory == null) {
            this.factory = DocumentBuilderFactory.newInstance();
        }
        String statusXML = vocabularyDAO.getVocabularyStatus(vocabulary.getId());
        statusXML = statusXML.replace("&lt;", "<");
        statusXML = statusXML.replace("&gt;", ">");
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            InputSource is = new InputSource(new StringReader(statusXML));
            Document document = builder.parse(is);
            dto.setOpNumber(document.getElementsByTagName("op_number").item(0).getTextContent());
            dto.setDescription(document.getElementsByTagName("description").item(0).getTextContent());
            dto.setStatus(document.getElementsByTagName("status").item(0).getTextContent());
            dto.setStatusName(getStatusNameForDTO(dto.getStatus()));
            dto.setDetail(document.getElementsByTagName("detail").item(0).getTextContent());
        } catch (ParserConfigurationException | SAXException | IOException e) {
            LOGGER.error("makeDTOWithCurrentStatus(Vocabulary vocabulary) failed.", e);
            return dto;
        }
        return dto;
    }

    private String getStatusNameForDTO(String status){
        switch (status){
            case "0":
                return VocabularyStatusDTO.BUILD_IN_PROGRESS;
            case "1":
                return VocabularyStatusDTO.READY;
            case "2":
                return VocabularyStatusDTO.READY_WITH_NOTICES;
            case "3":
                return VocabularyStatusDTO.FAILED;
            default:
                return VocabularyStatusDTO.NOT_AVAILABLE;
        }
    }

}