package org.example.knockin.mate.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.knockin.global.exception.BusinessException;
import org.example.knockin.global.exception.MyRoommateErrorCode;
import org.example.knockin.mate.dto.*;
import org.example.knockin.mate.dto.CalendarDto.CalendarInfoDto;
import org.example.knockin.mate.dto.CalendarEditDto.MemberInfo;
import org.example.knockin.mate.dto.MyRoommateDailyCalendarListDto.CalendarBasicInfo;
import org.example.knockin.mate.dto.RepeatCalendarDto.RepeatCalendarInfo;
import org.example.knockin.mate.dto.RepeatCalendarModifyDto.OriginalCalendar;
import org.example.knockin.mate.entity.*;
import org.example.knockin.mate.repository.row.DailyCalendarMemberRow;
import org.example.knockin.mate.repository.row.DailyCalendarRow;
import org.example.knockin.mate.repository.row.MonthlyCalendarRow;
import org.example.knockin.mate.repository.row.RepeatCalendarExcludeRow;
import org.example.knockin.member.entity.Member;
import org.example.knockin.member.repository.MemberRepository;
import org.example.knockin.member.repository.row.MemberWithNameRow;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CalendarServiceImpl {

    private final RoommateCalendarCategoryServiceImpl roommateCalendarCategoryService;
    private final RoommateCalendarServiceImpl roommateCalendarService;
    private final RoommateCalendarMemberServiceImpl roommateCalendarMemberService;
    private final RepeatRoommateCalendarServiceImpl repeatRoommateCalendarService;
    private final MemberRepository memberRepository;
    private final ExcludeRoommateCalendarServiceImpl excludeRoommateCalendarService;

    @Transactional
    public CalendarDto.Response saveBasic(Long memberId, MyRoommate myRoommate, CalendarDto.Request request) {
        RoommateMatchingRequired roommateMatchingRequired = myRoommate.getRoommateMatchingRequired();
        RoommateCalendarCategory roommateCalendarCategory = saveCalendarCategory(request.getCategoryName());
        RoommateCalendar roommateCalendar = saveCalendar(memberId, myRoommate, roommateCalendarCategory, request.getCalendar());
        saveCalendarMembers(roommateCalendar, roommateMatchingRequired, request.getMemberIds());
        return CalendarDto.Response.builder().updatedAt(LocalDateTime.now()).build();
    }

    private RoommateCalendar saveCalendar(Long memberId, MyRoommate myRoommate, RoommateCalendarCategory category, CalendarInfoDto dto) {
        RoommateMatchingRequired roommateMatchingRequired = myRoommate.getRoommateMatchingRequired();
        Member me = pickMe(memberId, roommateMatchingRequired);
        return roommateCalendarService.save(myRoommate, me, category, dto);
    }

    private RoommateCalendarCategory saveCalendarCategory(String name) {
        return roommateCalendarCategoryService.save(name);
    }

    private List<RoommateCalendarMember> saveCalendarMembers(RoommateCalendar calendar, RoommateMatchingRequired required, List<Long> memberIds) {
        List<Member> members = pickRoommateMembers(memberIds, required);
        return saveCalendarMembers(calendar, members);
    }

    private List<RoommateCalendarMember> saveCalendarMembers(RoommateCalendar calendar, List<Member> members) {
        return roommateCalendarMemberService.saveAll(calendar, members);
    }

    private List<Member> pickRoommateMembers(List<Long> memberIds, RoommateMatchingRequired required) {
        Member requester = required.getRequester();
        Member requestee = required.getRequestee();

        Map<Long, Member> roommateMembers = Map.of(
                requester.getId(), requester,
                requestee.getId(), requestee
        );

        return memberIds.stream()
                .map(memberId -> {
                    Member member = roommateMembers.get(memberId);
                    if (member == null) throw new BusinessException(MyRoommateErrorCode.CALENDER_ACCESS_DENIED);
                    return member;
                })
                .toList();
    }

    private Member pickMe(Long memberId, RoommateMatchingRequired roommateMatchingRequired) {
        Member requester = roommateMatchingRequired.getRequester();
        Member requestee = roommateMatchingRequired.getRequestee();
        return Objects.equals(requester.getId(), memberId) ? requester : requestee;
    }

    @Transactional
    public RepeatCalendarDto.Response saveRepeat(Long memberId, MyRoommate myRoommate, RepeatCalendarDto.Request request) {
        RoommateMatchingRequired roommateMatchingRequired = myRoommate.getRoommateMatchingRequired();
        RoommateCalendarCategory roommateCalendarCategory = saveCalendarCategory(request.getCategoryName());
        RoommateCalendar roommateCalendar = saveCalendar(memberId, myRoommate, roommateCalendarCategory, request.getCalendar());
        saveCalendarMembers(roommateCalendar, roommateMatchingRequired, request.getMemberIds());
        saveRepeatRoommateCalendar(roommateCalendar, request.getRepeatInfo());
        return RepeatCalendarDto.Response.builder().updatedAt(LocalDateTime.now()).build();
    }

    private RepeatRoommateCalendar saveRepeatRoommateCalendar(RoommateCalendar calendar, RepeatCalendarInfo repeatInfo) {
        return repeatRoommateCalendarService.save(calendar, repeatInfo);
    }

    @Transactional
    public MyRoommateDailyCalendarListDto.Response findDailyList(MyRoommate myRoommate, Integer year, Integer month, Integer day) {
        LocalDate targetDate = LocalDate.of(year, month, day);
        LocalDateTime from = targetDate.atStartOfDay();
        LocalDateTime to = targetDate.plusDays(1).atStartOfDay();
        List<DailyCalendarRow> dailyCalendarList = roommateCalendarService.findDailyCalendarList(myRoommate.getId(), from, to);

        List<Long> calendarIds = dailyCalendarList.stream()
                .map(DailyCalendarRow::calendarId)
                .distinct()
                .toList();
        Map<Long, List<MyRoommateDailyCalendarListDto.CalendarMember>> memberMap = findDailyCalendarMemberMap(calendarIds);

        List<Long> repeatCalendarIds = dailyCalendarList.stream()
                .map(DailyCalendarRow::repeatCalendarId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, Set<LocalDateTime>> excludeMap = findRepeatExcludeMap(repeatCalendarIds);

        List<MyRoommateDailyCalendarListDto.CalendarItem> calendars = dailyCalendarList.stream()
                .flatMap(calendar -> toDailyCalendarItems(calendar, from, to, memberMap, excludeMap).stream())
                .sorted(Comparator
                        .comparing((MyRoommateDailyCalendarListDto.CalendarItem item) -> item.getCalendarBasicInfo().getStartDate())
                        .thenComparing(item -> item.getCalendarBasicInfo().getCalendarId()))
                .toList();

        return MyRoommateDailyCalendarListDto.Response.builder()
                .targetDay(targetDate)
                .calendars(calendars)
                .build();
    }

    private Map<Long, List<MyRoommateDailyCalendarListDto.CalendarMember>> findDailyCalendarMemberMap(List<Long> calendarIds) {
        if (calendarIds.isEmpty()) {
            return Map.of();
        }

        return roommateCalendarMemberService.findDailyCalendarMembers(calendarIds).stream()
                .collect(Collectors.groupingBy(
                        DailyCalendarMemberRow::calendarId,
                        Collectors.mapping(
                                row -> MyRoommateDailyCalendarListDto.CalendarMember.builder()
                                        .memberId(row.memberId())
                                        .name(row.name())
                                        .build(),
                                Collectors.toList()
                        )
                ));
    }

    private Map<Long, Set<LocalDateTime>> findRepeatExcludeMap(List<Long> repeatCalendarIds) {
        if (repeatCalendarIds.isEmpty()) {
            return Map.of();
        }

        return excludeRoommateCalendarService.findRepeatCalendarExcludes(repeatCalendarIds).stream()
                .collect(Collectors.groupingBy(
                        RepeatCalendarExcludeRow::repeatCalendarId,
                        Collectors.mapping(RepeatCalendarExcludeRow::excludeAt, Collectors.toSet())
                ));
    }

    private List<MyRoommateDailyCalendarListDto.CalendarItem> toDailyCalendarItems(
            DailyCalendarRow calendar,
            LocalDateTime from,
            LocalDateTime to,
            Map<Long, List<MyRoommateDailyCalendarListDto.CalendarMember>> memberMap,
            Map<Long, Set<LocalDateTime>> excludeMap
    ) {
        List<MyRoommateDailyCalendarListDto.CalendarMember> members = memberMap.getOrDefault(calendar.calendarId(), Collections.emptyList());
        if (!calendar.isRepeat()) {
            return List.of(toDailyCalendarItem(calendar, calendar.startDate(), calendar.endDate(), members));
        }

        Set<LocalDateTime> excludedStartDates = excludeMap.getOrDefault(calendar.repeatCalendarId(), Collections.emptySet());
        List<MyRoommateDailyCalendarListDto.CalendarItem> calendarItems = new ArrayList<>();
        Duration duration = Duration.between(calendar.startDate(), calendar.endDate());
        LocalDateTime occurrenceStartDate = calendar.startDate();

        while (!occurrenceStartDate.isAfter(calendar.repeatEndDate()) && occurrenceStartDate.isBefore(to)) {
            LocalDateTime occurrenceEndDate = occurrenceStartDate.plus(duration);
            if (occurrenceEndDate.isAfter(from)
                    && occurrenceStartDate.isBefore(to)
                    && !excludedStartDates.contains(occurrenceStartDate)) {
                calendarItems.add(toDailyCalendarItem(calendar, occurrenceStartDate, occurrenceEndDate, members));
            }
            occurrenceStartDate = nextOccurrenceStartDate(occurrenceStartDate, calendar.repeatType());
        }

        return calendarItems;
    }

    private MyRoommateDailyCalendarListDto.CalendarItem toDailyCalendarItem(
            DailyCalendarRow calendar,
            LocalDateTime startDate,
            LocalDateTime endDate,
            List<MyRoommateDailyCalendarListDto.CalendarMember> members
    ) {
        CalendarBasicInfo calendarBasicInfo = CalendarBasicInfo.builder()
                .calendarId(calendar.calendarId())
                .title(calendar.title())
                .contents(calendar.contents())
                .isAllDay(startDate.equals(endDate))
                .startDate(startDate)
                .endDate(endDate)
                .categoryName(calendar.categoryName())
                .repeatType(calendar.repeatType())
                .build();

        return MyRoommateDailyCalendarListDto.CalendarItem.builder()
                .calendarBasicInfo(calendarBasicInfo)
                .calendarMembers(members)
                .build();
    }

    private LocalDateTime nextOccurrenceStartDate(LocalDateTime startDate, RepeatType repeatType) {
        return switch (repeatType) {
            case WEEKLY -> startDate.plusWeeks(1);
            case BI_WEEKLY -> startDate.plusWeeks(2);
            case MONTHLY -> startDate.plusMonths(1);
        };
    }

    @Transactional
    public MyRoommateMonthlyCalendarListDto.Response findMyMonthlyList(MyRoommate myRoommate, Integer year, Integer month) {
        YearMonth targetMonth = targetMonth(year, month);
        LocalDateTime from = targetMonth.atDay(1).atStartOfDay();
        LocalDateTime to = targetMonth.plusMonths(1).atDay(1).atStartOfDay();
        List<MonthlyCalendarRow> calendarCandidates = roommateCalendarService.findMonthlyCalendarList(myRoommate.getId(), from, to);

        List<Long> repeatCalendarIds = calendarCandidates.stream()
                .map(MonthlyCalendarRow::repeatCalendarId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, Set<LocalDateTime>> excludeMap = findRepeatExcludeMap(repeatCalendarIds);

        Set<LocalDate> existingDates = findExistingDates(calendarCandidates, from, to, excludeMap);
        List<MyRoommateMonthlyCalendarListDto.CalendarDay> calendarDays = targetMonthDays(targetMonth).stream()
                .map(targetDate -> MyRoommateMonthlyCalendarListDto.CalendarDay.builder()
                        .targetDate(targetDate)
                        .exists(existingDates.contains(targetDate))
                        .build())
                .toList();

        return MyRoommateMonthlyCalendarListDto.Response.builder()
                .targetMonth(targetMonth)
                .calendarDays(calendarDays)
                .build();
    }

    private YearMonth targetMonth(Integer year, Integer month) {
        YearMonth now = YearMonth.now();
        return YearMonth.of(
                year != null ? year : now.getYear(),
                month != null ? month : now.getMonthValue()
        );
    }

    private List<LocalDate> targetMonthDays(YearMonth targetMonth) {
        return IntStream.rangeClosed(1, targetMonth.lengthOfMonth())
                .mapToObj(targetMonth::atDay)
                .toList();
    }

    private Set<LocalDate> findExistingDates(
            List<MonthlyCalendarRow> calendarCandidates,
            LocalDateTime from,
            LocalDateTime to,
            Map<Long, Set<LocalDateTime>> excludeMap
    ) {
        Set<LocalDate> existingDates = new HashSet<>();
        calendarCandidates.forEach(calendar -> addExistingDates(calendar, from, to, excludeMap, existingDates));
        return existingDates;
    }

    private void addExistingDates(
            MonthlyCalendarRow calendar,
            LocalDateTime from,
            LocalDateTime to,
            Map<Long, Set<LocalDateTime>> excludeMap,
            Set<LocalDate> existingDates
    ) {
        if (!calendar.isRepeat()) {
            addOverlappedDates(calendar.startDate(), calendar.endDate(), from, to, existingDates);
            return;
        }

        Set<LocalDateTime> excludedStartDates = excludeMap.getOrDefault(calendar.repeatCalendarId(), Collections.emptySet());
        Duration duration = Duration.between(calendar.startDate(), calendar.endDate());
        LocalDateTime occurrenceStartDate = calendar.startDate();

        while (!occurrenceStartDate.isAfter(calendar.repeatEndDate()) && occurrenceStartDate.isBefore(to)) {
            LocalDateTime occurrenceEndDate = occurrenceStartDate.plus(duration);
            if (!excludedStartDates.contains(occurrenceStartDate)) {
                addOverlappedDates(occurrenceStartDate, occurrenceEndDate, from, to, existingDates);
            }
            occurrenceStartDate = nextOccurrenceStartDate(occurrenceStartDate, calendar.repeatType());
        }
    }

    private void addOverlappedDates(
            LocalDateTime startDate,
            LocalDateTime endDate,
            LocalDateTime from,
            LocalDateTime to,
            Set<LocalDate> existingDates
    ) {
        from.toLocalDate()
                .datesUntil(to.toLocalDate())
                .filter(date -> isOverlapping(startDate, endDate, date))
                .forEach(existingDates::add);
    }

    private boolean isOverlapping(LocalDateTime startDate, LocalDateTime endDate, LocalDate targetDate) {
        LocalDateTime dayStart = targetDate.atStartOfDay();
        LocalDateTime dayEnd = targetDate.plusDays(1).atStartOfDay();
        return endDate.isAfter(dayStart) && startDate.isBefore(dayEnd);
    }

    //TODO: 반복 일정 설정값 전달, 권한 체크, 응답에 기존값 포함
    @Transactional
    public CalendarEditDto.Response getEditForm(Long memberId, MyRoommate myRoommate) {
        RoommateMatchingRequired roommateMatchingRequired = myRoommate.getRoommateMatchingRequired();
        List<MemberInfo> memberInfos = findAllMemberInfo(memberId, roommateMatchingRequired);
        List<String> categoryNames = findCategoryNames();
        return CalendarEditDto.Response.builder()
                .repeatType(List.of(RepeatType.values()))
                .members(memberInfos)
                .categoryNames(categoryNames)
                .build();
    }

    private List<MemberInfo> findAllMemberInfo(Long myId, RoommateMatchingRequired roommateMatchingRequired) {
        List<Long> ids = List.of(roommateMatchingRequired.getRequester().getId(), roommateMatchingRequired.getRequestee().getId());
        List<MemberWithNameRow> rows = memberRepository.findAllWithNameRowById(ids);
        return rows.stream()
                .map(row -> toEditDto(myId, row))
                .toList();
    }

    private MemberInfo toEditDto(Long myId, MemberWithNameRow row) {
        return MemberInfo.builder()
                .memberId(row.id())
                .name(row.name())
                .isMe(Objects.equals(myId, row.id()))
                .build();
    }

    // TODO: 스펙 확정 후 수정
    public List<String> findCategoryNames() {
        return List.of("청소", "공과금", "기타");
    }

    @Transactional
    public CalendarDto.Response modifyCalendar(Long memberId, Long calendarId, CalendarDto.Request request) {
        RoommateCalendar roommateCalendar = roommateCalendarService.findByIdOrThrow(calendarId);

        if (!roommateCalendar.isOwner(memberId)) {
            throw new BusinessException(MyRoommateErrorCode.CALENDER_ACCESS_DENIED);
        }

        roommateCalendar.modify(request.getCalendar());
        modifyCalendarCategory(roommateCalendar, request.getCategoryName());
        modifyCalendarMember(roommateCalendar, request.getMemberIds());

        return CalendarDto.Response.builder().updatedAt(LocalDateTime.now()).build();
    }

    // TODO: 스펙 확정 후 수정
    private void modifyCalendarCategory(RoommateCalendar calendar, String categoryName) {
        RoommateCalendarCategory roommateCalendarCategory = calendar.getRoommateCalendarCategory();
        roommateCalendarCategory.rename(categoryName);
    }

    private void modifyCalendarMember(RoommateCalendar calendar, List<Long> requestMemberIds) {
        List<Member> requestedMembers = pickRoommateMembers(
                requestMemberIds,
                calendar.getMyRoommate().getRoommateMatchingRequired()
        );

        List<RoommateCalendarMember> calendarMembers = roommateCalendarMemberService.findByRoommateCalendar(calendar);
        List<Long> existingMemberIds = calendarMembers.stream().map(RoommateCalendarMember::getMemberId).toList();

        Set<Long> existingIdSet = new HashSet<>(existingMemberIds);
        Set<Long> requestIdSet = new HashSet<>(requestMemberIds);

        List<Member> membersToAdd = requestedMembers.stream()
                .filter(member -> !existingIdSet.contains(member.getId()))
                .toList();

        List<Long> idsToRemove = existingMemberIds.stream()
                .filter(id -> !requestIdSet.contains(id))
                .toList();

        saveCalendarMembers(calendar, membersToAdd);
        idsToRemove.forEach(memberId -> deleteCalendarMember(calendar.getId(), memberId));
    }

    private void deleteCalendarMember(Long roommateCalendarId, Long memberId) {
        roommateCalendarMemberService.deleteByCalendarIdAndMemberId(roommateCalendarId, memberId);
    }

    @Transactional
    public RepeatCalendarModifyDto.Response modifyRepeat(Long memberId, Long calendarId, MyRoommate myRoommate, RepeatCalendarModifyDto.Request request) {
        RoommateCalendar calendar = roommateCalendarService.findByIdOrThrow(calendarId);
        if (!calendar.isOwner(memberId)) throw new BusinessException(MyRoommateErrorCode.CALENDER_ACCESS_DENIED);

        RepeatRoommateCalendar repeatCalendar = repeatRoommateCalendarService.findOneByRoommateCalendarOrThrow(calendar);

        RepeatCalendarModifyType modifyType = request.getModifyType();
        switch (modifyType) {
            case THIS -> modifyOnlyThisRepeatCalendar(memberId, myRoommate, repeatCalendar, request);
            case THIS_AND_FOLLOWING -> modifyThisAndFollowingRepeatCalendar(memberId, myRoommate, repeatCalendar, request);
            case ALL -> modifyAllRepeatCalendar(calendar, repeatCalendar, request);
        }

        return RepeatCalendarModifyDto.Response.builder().updatedAt(LocalDateTime.now()).build();
    }

    private void modifyOnlyThisRepeatCalendar(Long memberId, MyRoommate myRoommate, RepeatRoommateCalendar repeatCalendar, RepeatCalendarModifyDto.Request request) {
        RoommateMatchingRequired roommateMatchingRequired = myRoommate.getRoommateMatchingRequired();
        RoommateCalendarCategory roommateCalendarCategory = saveCalendarCategory(request.getCategoryName());
        RoommateCalendar newCalendar = saveCalendar(memberId, myRoommate, roommateCalendarCategory, request.getCalendar());
        saveCalendarMembers(newCalendar, roommateMatchingRequired, request.getMemberIds());
        excludeRoommateCalendarService.save(repeatCalendar, request.getOriginalCalendar().getStartDate());
    }

    private void modifyThisAndFollowingRepeatCalendar(Long memberId, MyRoommate myRoommate, RepeatRoommateCalendar repeatCalendar, RepeatCalendarModifyDto.Request request) {
        LocalDateTime updatedEndDate = previousOccurrenceEndDate(request.getOriginalCalendar(), repeatCalendar.getRepeatType());
        repeatCalendar.modify(updatedEndDate, repeatCalendar.getRepeatType());

        RoommateCalendarCategory category = saveCalendarCategory(request.getCategoryName());
        RoommateCalendar newCalendar = saveCalendar(memberId, myRoommate, category, request.getCalendar());
        saveCalendarMembers(newCalendar, myRoommate.getRoommateMatchingRequired(), request.getMemberIds());

        saveRepeatRoommateCalendar(newCalendar, request.getRepeatInfo());
    }

    private void modifyAllRepeatCalendar(RoommateCalendar calendar, RepeatRoommateCalendar repeatCalendar, RepeatCalendarModifyDto.Request request) {
        CalendarInfoDto calendarInfo = applyOriginalOccurrenceDelta(calendar, request);
        calendar.modify(calendarInfo);
        modifyCalendarCategory(calendar, request.getCategoryName());
        modifyCalendarMember(calendar, request.getMemberIds());

        RepeatCalendarInfo repeatInfo = request.getRepeatInfo();
        repeatCalendar.modify(repeatInfo.getEndDate(), repeatInfo.getRepeatType());
    }

    private CalendarInfoDto applyOriginalOccurrenceDelta(RoommateCalendar calendar, RepeatCalendarModifyDto.Request request) {
        OriginalCalendar originalCalendarDto = request.getOriginalCalendar();
        CalendarInfoDto calendarDto = request.getCalendar();

        Duration startDelta = Duration.between(originalCalendarDto.getStartDate(), calendarDto.getStartDate());
        Duration endDelta = Duration.between(originalCalendarDto.getEndDate(), calendarDto.getEndDate());

        return CalendarInfoDto.builder()
                .myRoommateId(calendarDto.getMyRoommateId())
                .title(calendarDto.getTitle())
                .contents(calendarDto.getContents())
                .startDate(calendar.getStartDate().plus(startDelta))
                .endDate(calendar.getEndDate().plus(endDelta))
                .build();
    }

    private LocalDateTime previousOccurrenceEndDate(OriginalCalendar originalCalendar, RepeatType repeatType) {
        Duration duration = Duration.between(originalCalendar.getStartDate(), originalCalendar.getEndDate());
        LocalDateTime previousStartDate = switch (repeatType) {
            case WEEKLY -> originalCalendar.getStartDate().minusWeeks(1);
            case BI_WEEKLY -> originalCalendar.getStartDate().minusWeeks(2);
            case MONTHLY -> originalCalendar.getStartDate().minusMonths(1);
        };
        return previousStartDate.plus(duration);
    }

    @Transactional
    public CalendarDto.Response delete(Long memberId, Long calendarId) {
        RoommateCalendar calendar = roommateCalendarService.findByIdOrThrow(calendarId);

        if (!calendar.isOwner(memberId)) {
            throw new BusinessException(MyRoommateErrorCode.CALENDER_ACCESS_DENIED);
        }

        calendar.softDelete();
        return CalendarDto.Response.builder().updatedAt(LocalDateTime.now()).build();
    }
}
