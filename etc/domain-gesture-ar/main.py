from __future__ import annotations

import math
import random
import time
from dataclasses import dataclass
from typing import Callable, Sequence

import cv2
import mediapipe as mp
import numpy as np

WINDOW_NAME = "Domain Gesture AR"
HOLD_SECONDS = 0.8
EFFECT_SECONDS = 4.0
CAMERA_INDEX = 0

# 손바닥 교차(붉은 신전) 판정에 쓰는, 기본 hands_are_close보다 훨씬 타이트한 임계값
CROSSED_WRIST_DISTANCE = 0.18
CROSSED_INDEX_DISTANCE = 0.15
# 손가락 원(황금빛 심판) 판정: 엄지-검지 끝 사이 거리
FINGER_CIRCLE_DISTANCE = 0.06
# 양손 아래로 펼침(검은 그림자 정원) 판정: 손목이 화면 아래쪽 절반보다 더 아래에 있어야 함
LOWERED_WRIST_Y = 0.6


@dataclass
class Particle:
    angle: float
    speed: float
    radius: float
    size: int


@dataclass
class Theme:
    """영역 효과 하나의 색상·문구 팔레트. 렌더링 로직은 AreaEffect가 공유하고 팔레트만 바뀐다."""

    title: str
    subtitle: str
    dark_tint: tuple[int, int, int]
    shockwave_outer: tuple[int, int, int]
    shockwave_inner: tuple[int, int, int]
    ring_color_base: tuple[int, int, int]
    ring_point_color: tuple[int, int, int]
    symbol_color: tuple[int, int, int]
    particle_rb: tuple[int, int]
    particle_green_range: tuple[int, int]
    edge_color: tuple[int, int, int]
    title_color: tuple[int, int, int]
    subtitle_color: tuple[int, int, int]


class AreaEffect:
    """영역 전개풍 화면 효과를 관리한다. 색상·문구는 Theme으로 주입받는다."""

    def __init__(self) -> None:
        self.active = False
        self.started_at = 0.0
        self.particles: list[Particle] = []
        self.theme: Theme | None = None

    def start(self, theme: Theme) -> None:
        self.active = True
        self.theme = theme
        self.started_at = time.monotonic()
        self.particles = [
            Particle(
                angle=random.uniform(0, math.tau),
                speed=random.uniform(70, 260),
                radius=random.uniform(0, 80),
                size=random.randint(1, 5),
            )
            for _ in range(150)
        ]

    def stop(self) -> None:
        self.active = False
        self.particles.clear()

    def draw(self, frame: np.ndarray) -> np.ndarray:
        if not self.active or self.theme is None:
            return frame
        theme = self.theme
        elapsed = time.monotonic() - self.started_at
        if elapsed >= EFFECT_SECONDS:
            self.stop()
            return frame

        height, width = frame.shape[:2]
        center = (width // 2, height // 2)

        # 발동 초기 진행률
        intro_progress = min(elapsed / 0.8, 1.0)

        # 전체 화면을 테마 색상으로 어둡게 변경
        dark_layer = np.zeros_like(frame)
        dark_layer[:] = theme.dark_tint
        frame = cv2.addWeighted(
            frame,
            0.42,
            dark_layer,
            0.58,
            0,
        )

        overlay = frame.copy()

        # 중앙에서 퍼져나가는 충격파
        shockwave_radius = int(intro_progress * max(width, height) * 0.75)
        cv2.circle(
            overlay,
            center,
            shockwave_radius,
            theme.shockwave_outer,
            thickness=4,
            lineType=cv2.LINE_AA,
        )
        cv2.circle(
            overlay,
            center,
            max(1, shockwave_radius - 15),
            theme.shockwave_inner,
            thickness=2,
            lineType=cv2.LINE_AA,
        )

        # 회전하는 마법진
        rotation = elapsed * 1.7
        ring_base = theme.ring_color_base
        for ring_index, base_radius in enumerate((90, 145, 210)):
            radius = int(base_radius * intro_progress)
            cv2.circle(
                overlay,
                center,
                radius,
                (ring_base[0], ring_base[1] + ring_index * 30, ring_base[2]),
                thickness=2,
                lineType=cv2.LINE_AA,
            )
            point_count = 8 + ring_index * 4
            for point_index in range(point_count):
                angle = (
                    math.tau * point_index / point_count
                    + rotation * (1 if ring_index % 2 == 0 else -1)
                )
                x = int(center[0] + math.cos(angle) * radius)
                y = int(center[1] + math.sin(angle) * radius)
                cv2.circle(
                    overlay,
                    (x, y),
                    3 + ring_index,
                    theme.ring_point_color,
                    thickness=-1,
                    lineType=cv2.LINE_AA,
                )

        # 중앙 문양
        symbol_radius = int(60 * intro_progress)
        for index in range(6):
            angle = rotation + index * math.pi / 3
            start = (
                int(center[0] + math.cos(angle) * 15),
                int(center[1] + math.sin(angle) * 15),
            )
            end = (
                int(center[0] + math.cos(angle) * symbol_radius),
                int(center[1] + math.sin(angle) * symbol_radius),
            )
            cv2.line(
                overlay,
                start,
                end,
                theme.symbol_color,
                3,
                cv2.LINE_AA,
            )

        # 바깥쪽으로 퍼지는 입자
        particle_r, particle_b = theme.particle_rb
        green_low, green_high = theme.particle_green_range
        for particle in self.particles:
            travel_distance = particle.radius + particle.speed * elapsed
            x = int(center[0] + math.cos(particle.angle) * travel_distance)
            y = int(center[1] + math.sin(particle.angle) * travel_distance)
            if 0 <= x < width and 0 <= y < height:
                cv2.circle(
                    overlay,
                    (x, y),
                    particle.size,
                    (particle_r, random.randint(green_low, green_high), particle_b),
                    thickness=-1,
                    lineType=cv2.LINE_AA,
                )

        # 화면 가장자리 왜곡처럼 보이는 선
        for index in range(18):
            angle = math.tau * index / 18 + rotation * 0.3
            outer_radius = int(max(width, height) * 0.7)
            inner_radius = int(240 + 15 * math.sin(elapsed * 5 + index))
            start = (
                int(center[0] + math.cos(angle) * inner_radius),
                int(center[1] + math.sin(angle) * inner_radius),
            )
            end = (
                int(center[0] + math.cos(angle) * outer_radius),
                int(center[1] + math.sin(angle) * outer_radius),
            )
            cv2.line(
                overlay,
                start,
                end,
                theme.edge_color,
                1,
                cv2.LINE_AA,
            )

        frame = cv2.addWeighted(frame, 0.4, overlay, 0.8, 0)
        self._draw_title(frame, elapsed)
        return frame

    def _draw_title(self, frame: np.ndarray, elapsed: float) -> None:
        if self.theme is None:
            return
        theme = self.theme
        height, width = frame.shape[:2]

        # 발동 후 잠깐 뒤에 문구 표시
        if elapsed < 0.35:
            return

        title = theme.title
        subtitle = theme.subtitle

        title_scale = min(width / 900, 1.2)
        title_size = cv2.getTextSize(
            title,
            cv2.FONT_HERSHEY_DUPLEX,
            title_scale,
            2,
        )[0]
        title_x = (width - title_size[0]) // 2
        title_y = height - 95

        # 그림자
        cv2.putText(
            frame,
            title,
            (title_x + 3, title_y + 3),
            cv2.FONT_HERSHEY_DUPLEX,
            title_scale,
            (0, 0, 0),
            5,
            cv2.LINE_AA,
        )
        cv2.putText(
            frame,
            title,
            (title_x, title_y),
            cv2.FONT_HERSHEY_DUPLEX,
            title_scale,
            theme.title_color,
            2,
            cv2.LINE_AA,
        )

        subtitle_size = cv2.getTextSize(
            subtitle,
            cv2.FONT_HERSHEY_SIMPLEX,
            0.65,
            1,
        )[0]
        subtitle_x = (width - subtitle_size[0]) // 2
        cv2.putText(
            frame,
            subtitle,
            (subtitle_x, title_y + 35),
            cv2.FONT_HERSHEY_SIMPLEX,
            0.65,
            theme.subtitle_color,
            1,
            cv2.LINE_AA,
        )


def distance(point_a, point_b) -> float:
    return math.hypot(
        point_a.x - point_b.x,
        point_a.y - point_b.y,
    )


def finger_is_extended(
    landmarks,
    tip_index: int,
    pip_index: int,
    mcp_index: int,
) -> bool:
    """
    손목에서 손가락 끝까지의 거리가
    손목에서 중간 관절까지의 거리보다 충분히 길면 펼쳐진 것으로 판단한다.
    손 회전에도 비교적 대응하기 위해 단순 y좌표가 아닌 거리 기반으로 판정한다.
    """
    wrist = landmarks[0]
    tip_distance = distance(wrist, landmarks[tip_index])
    pip_distance = distance(wrist, landmarks[pip_index])
    mcp_distance = distance(wrist, landmarks[mcp_index])
    return (
        tip_distance > pip_distance * 1.08
        and tip_distance > mcp_distance * 1.25
    )


def matches_domain_gesture(landmarks) -> bool:
    """
    예제 영역 전개 자세:
    검지와 중지는 펼치고,
    약지와 새끼손가락은 접는다.
    엄지는 이번 MVP 판정에서 제외한다.
    """
    index_extended = finger_is_extended(
        landmarks,
        tip_index=8,
        pip_index=6,
        mcp_index=5,
    )
    middle_extended = finger_is_extended(
        landmarks,
        tip_index=12,
        pip_index=10,
        mcp_index=9,
    )
    ring_extended = finger_is_extended(
        landmarks,
        tip_index=16,
        pip_index=14,
        mcp_index=13,
    )
    pinky_extended = finger_is_extended(
        landmarks,
        tip_index=20,
        pip_index=18,
        mcp_index=17,
    )
    return (
        index_extended
        and middle_extended
        and not ring_extended
        and not pinky_extended
    )


def matches_open_palm(landmarks) -> bool:
    """네 손가락(검지~새끼)이 모두 펼쳐진, 손바닥을 편 자세인지 확인한다."""
    return (
        finger_is_extended(landmarks, 8, 6, 5)
        and finger_is_extended(landmarks, 12, 10, 9)
        and finger_is_extended(landmarks, 16, 14, 13)
        and finger_is_extended(landmarks, 20, 18, 17)
    )


def matches_finger_circle(landmarks) -> bool:
    """엄지 끝과 검지 끝이 맞닿아 원 모양을 이루는지 확인한다."""
    return distance(landmarks[4], landmarks[8]) < FINGER_CIRCLE_DISTANCE


def hands_are_close(
    first_hand,
    second_hand,
    wrist_threshold: float = 0.38,
    index_threshold: float = 0.30,
) -> bool:
    """
    두 손의 손목과 검지 시작점이 가까운지 확인한다.
    좌표가 0~1로 정규화돼 있으므로 화면 크기와 무관하게 사용할 수 있다.
    임계값을 인자로 받아, 자세별로 "가깝다"의 기준을 다르게 쓸 수 있게 한다.
    """
    wrist_distance = distance(first_hand[0], second_hand[0])
    index_base_distance = distance(first_hand[5], second_hand[5])
    return wrist_distance < wrist_threshold and index_base_distance < index_threshold


def matches_blue_void_gesture(hands: Sequence) -> bool:
    """푸른 무한 공간: 기존 영역 전개 자세(검지·중지만 펼침) + 양손을 가깝게 모음."""
    first, second = hands
    return (
        matches_domain_gesture(first)
        and matches_domain_gesture(second)
        and hands_are_close(first, second)
    )


def matches_crimson_shrine_gesture(hands: Sequence) -> bool:
    """붉은 신전: 양손 모두 손바닥을 펴고, 두 손을 거의 겹치도록 교차시킴."""
    first, second = hands
    return (
        matches_open_palm(first)
        and matches_open_palm(second)
        and hands_are_close(
            first,
            second,
            wrist_threshold=CROSSED_WRIST_DISTANCE,
            index_threshold=CROSSED_INDEX_DISTANCE,
        )
    )


def matches_shadow_garden_gesture(hands: Sequence) -> bool:
    """검은 그림자 정원: 양손 모두 손바닥을 펴고, 화면 아래쪽에서 서로 멀리 벌림."""
    first, second = hands
    return (
        matches_open_palm(first)
        and matches_open_palm(second)
        and not hands_are_close(first, second)
        and first[0].y > LOWERED_WRIST_Y
        and second[0].y > LOWERED_WRIST_Y
    )


def matches_golden_judgment_gesture(hands: Sequence) -> bool:
    """황금빛 심판 공간: 양손 모두 엄지·검지로 원(고리) 모양을 만듦."""
    first, second = hands
    return matches_finger_circle(first) and matches_finger_circle(second)


BLUE_VOID_THEME = Theme(
    title="BLUE INFINITE VOID",
    subtitle="ENDLESS ABYSS",
    dark_tint=(50, 20, 5),
    shockwave_outer=(255, 200, 80),
    shockwave_inner=(255, 100, 40),
    ring_color_base=(255, 120, 40),
    ring_point_color=(255, 230, 190),
    symbol_color=(255, 210, 130),
    particle_rb=(255, 60),
    particle_green_range=(120, 220),
    edge_color=(200, 70, 30),
    title_color=(255, 235, 210),
    subtitle_color=(255, 190, 140),
)

CRIMSON_SHRINE_THEME = Theme(
    title="CRIMSON SHRINE",
    subtitle="SACRED FLAME",
    dark_tint=(10, 10, 45),
    shockwave_outer=(40, 160, 255),
    shockwave_inner=(30, 40, 220),
    ring_color_base=(20, 90, 255),
    ring_point_color=(120, 190, 255),
    symbol_color=(60, 170, 255),
    particle_rb=(40, 255),
    particle_green_range=(60, 160),
    edge_color=(30, 30, 180),
    title_color=(210, 220, 255),
    subtitle_color=(120, 170, 255),
)

SHADOW_GARDEN_THEME = Theme(
    title="SHADOW GARDEN",
    subtitle="WHISPERING DARK",
    dark_tint=(15, 15, 15),
    shockwave_outer=(140, 60, 90),
    shockwave_inner=(70, 20, 40),
    ring_color_base=(90, 40, 60),
    ring_point_color=(150, 200, 140),
    symbol_color=(120, 80, 60),
    particle_rb=(60, 40),
    particle_green_range=(40, 120),
    edge_color=(70, 20, 30),
    title_color=(180, 220, 190),
    subtitle_color=(120, 160, 130),
)

GOLDEN_JUDGMENT_THEME = Theme(
    title="GOLDEN JUDGMENT",
    subtitle="SCALES OF LIGHT",
    dark_tint=(5, 25, 35),
    shockwave_outer=(60, 210, 255),
    shockwave_inner=(20, 160, 255),
    ring_color_base=(30, 180, 255),
    ring_point_color=(150, 235, 255),
    symbol_color=(50, 200, 255),
    particle_rb=(40, 255),
    particle_green_range=(180, 240),
    edge_color=(20, 140, 220),
    title_color=(210, 245, 255),
    subtitle_color=(60, 210, 255),
)

# 우선순위 순서로 검사한다: 원(손가락 원)과 도메인 자세(검지·중지)는 손가락 패턴만으로도
# 서로 구분되지만, 손바닥 교차/양손 벌림은 둘 다 "손바닥을 편 상태"가 조건이라 손 사이
# 거리·위치로만 구분된다. 더 구체적인 자세를 먼저 검사해 오검출을 줄인다.
GESTURE_LIBRARY: list[tuple[str, Callable[[Sequence], bool], Theme]] = [
    ("황금빛 심판 공간", matches_golden_judgment_gesture, GOLDEN_JUDGMENT_THEME),
    ("푸른 무한 공간", matches_blue_void_gesture, BLUE_VOID_THEME),
    ("붉은 신전", matches_crimson_shrine_gesture, CRIMSON_SHRINE_THEME),
    ("검은 그림자 정원", matches_shadow_garden_gesture, SHADOW_GARDEN_THEME),
]


def detect_active_gesture(hands: Sequence) -> tuple[str, Theme] | None:
    if len(hands) != 2:
        return None
    for label, matcher, theme in GESTURE_LIBRARY:
        if matcher(hands):
            return label, theme
    return None


def draw_activation_meter(
    frame: np.ndarray,
    progress: float,
    gesture_label: str | None,
) -> None:
    height, width = frame.shape[:2]
    bar_width = min(360, width - 80)
    bar_height = 18
    x = (width - bar_width) // 2
    y = height - 45

    cv2.rectangle(
        frame,
        (x, y),
        (x + bar_width, y + bar_height),
        (40, 40, 40),
        thickness=-1,
    )
    filled_width = int(bar_width * progress)
    cv2.rectangle(
        frame,
        (x, y),
        (x + filled_width, y + bar_height),
        (220, 120, 255),
        thickness=-1,
    )
    border_color = (255, 255, 255) if gesture_label else (130, 130, 130)
    cv2.rectangle(
        frame,
        (x, y),
        (x + bar_width, y + bar_height),
        border_color,
        thickness=2,
    )

    status = f"CHARGING: {gesture_label}" if gesture_label else "SHOW A GESTURE"
    cv2.putText(
        frame,
        status,
        (x, y - 10),
        cv2.FONT_HERSHEY_SIMPLEX,
        0.55,
        border_color,
        1,
        cv2.LINE_AA,
    )


def main() -> None:
    capture = cv2.VideoCapture(CAMERA_INDEX)
    if not capture.isOpened():
        raise RuntimeError(
            "웹캠을 열 수 없습니다. CAMERA_INDEX 또는 카메라 권한을 확인하세요."
        )
    capture.set(cv2.CAP_PROP_FRAME_WIDTH, 1280)
    capture.set(cv2.CAP_PROP_FRAME_HEIGHT, 720)

    mp_hands = mp.solutions.hands
    mp_drawing = mp.solutions.drawing_utils
    mp_styles = mp.solutions.drawing_styles

    effect = AreaEffect()
    gesture_started_at: float | None = None
    current_gesture_label: str | None = None
    trigger_locked = False

    with mp_hands.Hands(
        static_image_mode=False,
        max_num_hands=2,
        model_complexity=1,
        min_detection_confidence=0.65,
        min_tracking_confidence=0.65,
    ) as hands:
        while True:
            success, frame = capture.read()
            if not success:
                print("웹캠 프레임을 읽지 못했습니다.")
                break

            # 거울처럼 보이도록 좌우 반전
            frame = cv2.flip(frame, 1)
            rgb_frame = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
            rgb_frame.flags.writeable = False
            result = hands.process(rgb_frame)
            rgb_frame.flags.writeable = True

            detected_hands = []
            if result.multi_hand_landmarks:
                for hand_landmarks in result.multi_hand_landmarks:
                    detected_hands.append(hand_landmarks.landmark)
                    mp_drawing.draw_landmarks(
                        frame,
                        hand_landmarks,
                        mp_hands.HAND_CONNECTIONS,
                        mp_styles.get_default_hand_landmarks_style(),
                        mp_styles.get_default_hand_connections_style(),
                    )

            active_gesture = detect_active_gesture(detected_hands)

            now = time.monotonic()
            hold_progress = 0.0
            active_label = active_gesture[0] if active_gesture else None
            if active_gesture is not None and not effect.active:
                label, theme = active_gesture
                if gesture_started_at is None or current_gesture_label != label:
                    gesture_started_at = now
                    current_gesture_label = label
                held_seconds = now - gesture_started_at
                hold_progress = min(held_seconds / HOLD_SECONDS, 1.0)
                if held_seconds >= HOLD_SECONDS and not trigger_locked:
                    effect.start(theme)
                    trigger_locked = True
            else:
                gesture_started_at = None
                current_gesture_label = None
                # 손동작을 해제해야 다시 발동할 수 있도록 잠금 해제
                if active_gesture is None:
                    trigger_locked = False

            if effect.active:
                frame = effect.draw(frame)
            else:
                draw_activation_meter(
                    frame,
                    hold_progress,
                    active_label,
                )

            cv2.putText(
                frame,
                "Q: quit | R: manual activation",
                (20, 32),
                cv2.FONT_HERSHEY_SIMPLEX,
                0.65,
                (255, 255, 255),
                1,
                cv2.LINE_AA,
            )

            cv2.imshow(WINDOW_NAME, frame)
            key = cv2.waitKey(1) & 0xFF
            if key == ord("q"):
                break
            if key == ord("r"):
                _, random_theme = random.choice(
                    [(label, theme) for label, _, theme in GESTURE_LIBRARY]
                )
                effect.start(random_theme)

    capture.release()
    cv2.destroyAllWindows()


if __name__ == "__main__":
    main()
