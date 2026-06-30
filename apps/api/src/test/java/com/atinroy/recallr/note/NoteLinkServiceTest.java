package com.atinroy.recallr.note;

import com.atinroy.recallr.domain.note.*;
import com.atinroy.recallr.domain.note.dto.NoteLinkRequest;
import com.atinroy.recallr.domain.note.dto.NoteLinkResponse;
import com.atinroy.recallr.security.AuthenticatedUserProvider;
import com.atinroy.recallr.domain.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NoteLinkServiceTest {

    @Mock
    private AuthenticatedUserProvider authenticatedUserProvider;
    @Mock
    private NoteRepository noteRepository;
    @Mock
    private NoteLinkRepository noteLinkRepository;

    @InjectMocks
    private NoteLinkService noteLinkService;

    private User user;
    private Note sourceNote;
    private Note targetNote;

    @BeforeEach
    void setUp() {
        user = new User();
        sourceNote = new Note();
        targetNote = new Note();
    }

    @Test
    void createNoteLink_withValidSourceAndTarget_returnsResponseWithBothIds() {
        when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
        when(noteRepository.findByIdAndUserId(sourceNote.getId(), user.getId())).thenReturn(Optional.of(sourceNote));
        when(noteRepository.findByIdAndUserId(targetNote.getId(), user.getId())).thenReturn(Optional.of(targetNote));

        NoteLink savedLink = new NoteLink();
        savedLink.setSource(sourceNote);
        savedLink.setTarget(targetNote);
        when(noteLinkRepository.save(any(NoteLink.class))).thenReturn(savedLink);

        NoteLinkRequest request = new NoteLinkRequest(sourceNote.getId().toString(), targetNote.getId().toString());
        NoteLinkResponse response = noteLinkService.createNoteLink(request);

        assertThat(response.sourceId()).isEqualTo(sourceNote.getId());
        assertThat(response.targetId()).isEqualTo(targetNote.getId());
        assertThat(response.id()).isEqualTo(savedLink.getId());
    }

    @Test
    void createNoteLink_whenSourceNoteNotFound_throwsNoteNotFoundException() {
        when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
        when(noteRepository.findByIdAndUserId(sourceNote.getId(), user.getId())).thenReturn(Optional.empty());

        NoteLinkRequest request = new NoteLinkRequest(sourceNote.getId().toString(), targetNote.getId().toString());
        assertThatThrownBy(() -> noteLinkService.createNoteLink(request))
                .isInstanceOf(NoteNotFoundException.class);
    }

    @Test
    void createNoteLink_whenTargetNoteNotFound_throwsNoteNotFoundException() {
        when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
        when(noteRepository.findByIdAndUserId(sourceNote.getId(), user.getId())).thenReturn(Optional.of(sourceNote));
        when(noteRepository.findByIdAndUserId(targetNote.getId(), user.getId())).thenReturn(Optional.empty());

        NoteLinkRequest request = new NoteLinkRequest(sourceNote.getId().toString(), targetNote.getId().toString());
        assertThatThrownBy(() -> noteLinkService.createNoteLink(request))
                .isInstanceOf(NoteNotFoundException.class);
    }

    @Test
    void updateNoteLink_withValidId_updatesSourceAndTarget() {
        NoteLink existingLink = new NoteLink();
        existingLink.setSource(new Note());
        existingLink.setTarget(new Note());

        when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
        when(noteLinkRepository.findByIdAndSourceUserId(existingLink.getId(), user.getId()))
                .thenReturn(Optional.of(existingLink));
        when(noteRepository.findByIdAndUserId(sourceNote.getId(), user.getId())).thenReturn(Optional.of(sourceNote));
        when(noteRepository.findByIdAndUserId(targetNote.getId(), user.getId())).thenReturn(Optional.of(targetNote));
        when(noteLinkRepository.save(existingLink)).thenReturn(existingLink);

        NoteLinkRequest request = new NoteLinkRequest(sourceNote.getId().toString(), targetNote.getId().toString());
        NoteLinkResponse response = noteLinkService.updateNoteLink(existingLink.getId(), request);

        assertThat(response.sourceId()).isEqualTo(sourceNote.getId());
        assertThat(response.targetId()).isEqualTo(targetNote.getId());
    }

    @Test
    void updateNoteLink_whenLinkNotFound_throwsNoteLinkNotFoundException() {
        UUID linkId = UUID.randomUUID();
        when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
        when(noteLinkRepository.findByIdAndSourceUserId(linkId, user.getId())).thenReturn(Optional.empty());

        NoteLinkRequest request = new NoteLinkRequest(sourceNote.getId().toString(), targetNote.getId().toString());
        assertThatThrownBy(() -> noteLinkService.updateNoteLink(linkId, request))
                .isInstanceOf(NoteLinkNotFoundException.class);
    }

    @Test
    void deleteNoteLink_withValidId_deletesTheLink() {
        NoteLink existingLink = new NoteLink();
        when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
        when(noteLinkRepository.findByIdAndSourceUserId(existingLink.getId(), user.getId()))
                .thenReturn(Optional.of(existingLink));

        noteLinkService.deleteNoteLink(existingLink.getId());

        verify(noteLinkRepository).delete(existingLink);
    }

    @Test
    void deleteNoteLink_whenLinkNotFound_throwsNoteLinkNotFoundException() {
        UUID linkId = UUID.randomUUID();
        when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
        when(noteLinkRepository.findByIdAndSourceUserId(linkId, user.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> noteLinkService.deleteNoteLink(linkId))
                .isInstanceOf(NoteLinkNotFoundException.class);
    }
}
