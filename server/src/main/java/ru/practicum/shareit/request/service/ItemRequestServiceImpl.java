package ru.practicum.shareit.request.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.dto.ItemShortDto;
import ru.practicum.shareit.item.mapper.ItemMapper;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.request.dto.NewItemRequestDto;
import ru.practicum.shareit.request.mapper.ItemRequestMapper;
import ru.practicum.shareit.request.model.ItemRequest;
import ru.practicum.shareit.request.repository.ItemRequestRepository;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemRequestServiceImpl implements ItemRequestService {

    // списки запросов по ТЗ — от новых к старым; id разводит запросы, созданные в одно и то же время
    private static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "created", "id");

    private final ItemRequestRepository requestRepository;
    private final UserRepository userRepository;
    private final ItemRepository itemRepository;

    @Override
    @Transactional
    public ItemRequestDto create(Long userId, NewItemRequestDto requestDto) {
        User requestor = getUserOrThrow(userId);
        ItemRequest request = requestRepository.save(
                ItemRequestMapper.toItemRequest(requestDto, requestor, LocalDateTime.now()));
        log.info("Created item request id={} by user id={}", request.getId(), userId);
        // запрос только что создан, ответов на него ещё нет
        return ItemRequestMapper.toItemRequestDto(request, List.of());
    }

    @Override
    public List<ItemRequestDto> getOwn(Long userId) {
        checkUserExists(userId);
        return toDtosWithItems(requestRepository.findByRequestor_Id(userId, NEWEST_FIRST));
    }

    @Override
    public List<ItemRequestDto> getAllOthers(Long userId) {
        checkUserExists(userId);
        return toDtosWithItems(requestRepository.findByRequestor_IdNot(userId, NEWEST_FIRST));
    }

    // посмотреть отдельный запрос может любой пользователь, не только его автор
    @Override
    public ItemRequestDto getById(Long userId, Long requestId) {
        checkUserExists(userId);
        ItemRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Item request with id=" + requestId + " not found"));
        return toDtosWithItems(List.of(request)).get(0);
    }

    /**
     * Ответы берём сразу для всех запросов, одним обращением к базе, а не по одному на запрос:
     * иначе на список из N запросов ушёл бы N + 1 запрос к базе.
     */
    private List<ItemRequestDto> toDtosWithItems(List<ItemRequest> requests) {
        if (requests.isEmpty()) {
            return List.of();
        }
        List<Long> requestIds = requests.stream()
                .map(ItemRequest::getId)
                .toList();
        Map<Long, List<ItemShortDto>> itemsByRequestId = itemRepository.findByRequest_IdInOrderById(requestIds)
                .stream()
                .collect(Collectors.groupingBy(item -> item.getRequest().getId(),
                        Collectors.mapping(ItemMapper::toItemShortDto, Collectors.toList())));
        return requests.stream()
                .map(request -> ItemRequestMapper.toItemRequestDto(request,
                        itemsByRequestId.getOrDefault(request.getId(), List.of())))
                .toList();
    }

    private void checkUserExists(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("User with id=" + userId + " not found");
        }
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User with id=" + userId + " not found"));
    }
}
