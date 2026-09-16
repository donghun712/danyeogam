import { useEffect, useRef } from "react";
import { MAP_BOUNDS_DEBOUNCE_MS } from "@/constants/api";
import {
  createAttractionMarkerImage,
  createClustererStyle,
  createCurrentLocationMarkerImage,
} from "@/utils/attractionMarkerIcons";
import type { MapBounds } from "@/api/attractionApi";
import type { MapSpot } from "@/types/api";
import styles from "./MapView.module.css";

interface MapViewProps {
  currentPosition: { latitude: number; longitude: number } | null;
  spots: MapSpot[];
  onBoundsChange: (bounds: MapBounds) => void;
  onSelectSpot: (spotId: number) => void;
  /** 요청서 1.3 — Bottom Sheet가 열린 장소의 마커를 얇은 외곽 링으로 강조한다. */
  selectedSpotId: number | null;
  /**
   * 요청서 4단계 — "현재 위치" 버튼을 누르면 이 값이 바뀌어 그 순간의 currentPosition으로
   * 지도 중심을 이동한다. 0(초기값)에서는 아무 것도 안 하고, 버튼을 누를 때마다 부모가
   * 이 값을 1씩 증가시킨다.
   */
  recenterSignal: number;
  /**
   * 후속 요청서 1단계 — "현재 위치" 버튼을 카카오 기본 ZoomControl 바로 위, 같은
   * 우측 축으로 정렬해야 하는데, ZoomControl은 SDK가 직접 렌더링해서 정확한 크기/
   * 위치를 고정 px로 짐작할 수 없다(SDK 버전에 따라 달라질 수 있음). 그래서 실제
   * 렌더링된 컨트롤의 위치를 런타임에 측정해서 이 콜백으로 부모에 전달한다 —
   * 측정 실패 시(구조를 못 찾음) null을 전달해 부모가 기존 고정값으로 폴백한다.
   */
  onZoomControlRect: (rect: { right: number; top: number } | null) => void;
}

const DEFAULT_CENTER = { latitude: 35.8242, longitude: 127.148 }; // 전주 한옥마을 인근 — 최초 진입 시 GPS 확보 전 기본 좌표
const DEFAULT_LEVEL = 5;

// 요청서 3단계 — 상세 진입 후 뒤로가기 시 복원할 최소 지도 상태(center + level).
// 새 상태 관리 라이브러리 없이 sessionStorage만 사용 — 새로고침 후 영구 유지는 요구되지
// 않았고, 브라우저 뒤로가기(탭 유지) 동안만 살아있으면 된다.
const MAP_STATE_STORAGE_KEY = "danyeogam:map-view-state";

interface SavedMapState {
  lat: number;
  lng: number;
  level: number;
}

function readSavedMapState(): SavedMapState | null {
  try {
    const raw = sessionStorage.getItem(MAP_STATE_STORAGE_KEY);
    if (!raw) return null;
    const parsed = JSON.parse(raw) as Partial<SavedMapState>;
    if (
      typeof parsed.lat !== "number" ||
      typeof parsed.lng !== "number" ||
      typeof parsed.level !== "number"
    ) {
      return null;
    }
    return { lat: parsed.lat, lng: parsed.lng, level: parsed.level };
  } catch {
    return null;
  }
}

/**
 * 카카오맵 인스턴스 자체는 React 상태로 관리하지 않고 ref에 보관한다(SDK가 명령형 API이기 때문).
 * spots/currentPosition이 바뀔 때마다 마커만 다시 그린다.
 */
export function MapView({
  currentPosition,
  spots,
  onBoundsChange,
  onSelectSpot,
  selectedSpotId,
  recenterSignal,
  onZoomControlRect,
}: MapViewProps) {
  const containerRef = useRef<HTMLDivElement | null>(null);
  const mapRef = useRef<kakao.maps.Map | null>(null);
  const clustererRef = useRef<kakao.maps.MarkerClusterer | null>(null);
  const markersRef = useRef<kakao.maps.Marker[]>([]);
  const currentLocationMarkerRef = useRef<kakao.maps.Marker | null>(null);
  // 요청서 3.5 — 복원된 지도 상태가 있으면 이 값을 true로 미리 설정해서, 최초 GPS
  // 확보 effect가 복원 위치를 덮어쓰지 않게 한다("복원 상태 있음 -> GPS recenter 안 함").
  const hasCenteredOnGpsRef = useRef(false);
  const debounceTimerRef = useRef<number | null>(null);
  const resizeFrameRef = useRef<number | null>(null);
  const onBoundsChangeRef = useRef(onBoundsChange);
  const onSelectSpotRef = useRef(onSelectSpot);
  const onZoomControlRectRef = useRef(onZoomControlRect);
  const lastRecenterSignalRef = useRef(recenterSignal);

  // 렌더링 중에는 ref를 쓰지 않고, 콜백이 바뀔 때만 이펙트에서 최신 값을 동기화한다.
  useEffect(() => {
    onBoundsChangeRef.current = onBoundsChange;
  }, [onBoundsChange]);
  useEffect(() => {
    onSelectSpotRef.current = onSelectSpot;
  }, [onSelectSpot]);
  useEffect(() => {
    onZoomControlRectRef.current = onZoomControlRect;
  }, [onZoomControlRect]);

  // 지도 최초 생성 — 한 번만 실행
  useEffect(() => {
    if (!containerRef.current || mapRef.current) return;
    const container = containerRef.current;

    const savedState = readSavedMapState();
    const initialCenter = savedState
      ? { latitude: savedState.lat, longitude: savedState.lng }
      : DEFAULT_CENTER;
    const initialLevel = savedState ? savedState.level : DEFAULT_LEVEL;

    const map = new kakao.maps.Map(container, {
      center: new kakao.maps.LatLng(
        initialCenter.latitude,
        initialCenter.longitude,
      ),
      level: initialLevel,
    });
    mapRef.current = map;

    // 복원할 상태가 있었다면 최초 GPS 확보 시 다시 그쪽으로 이동하지 않게 한다.
    if (savedState) {
      hasCenteredOnGpsRef.current = true;
    }

    // 화면시안 우측 하단 확대/축소 컨트롤 — 카카오맵 SDK 기본 제공 컨트롤을 그대로 사용한다.
    // SDK에 나침반(지도 회전) 컨트롤은 별도로 제공되지 않아 추가하지 않았다(아래 보고 참고).
    map.addControl(
      new kakao.maps.ZoomControl(),
      kakao.maps.ControlPosition.BOTTOMRIGHT,
    );

    // 후속 요청서 1단계 — ZoomControl은 SDK가 직접 DOM을 그려서 정확한 크기/위치를
    // 고정 px로 짐작할 수 없다. 실제 렌더링된 컨트롤을 찾아 지도 컨테이너 기준
    // 상대 좌표(right, top)로 변환해 부모에 전달한다 — "현재 위치" 버튼이 그 바로
    // 위, 같은 우측 축에 정렬되도록. 카카오 컨트롤 고유 클래스명이 공식 문서에
    // 없어서, "작고(<=60px 폭) 세로로 긴(40~150px 높이) absolute 배치된 컨테이너"
    // 라는 형태적 특징으로 찾는다 — 못 찾으면 null을 전달해 부모가 기존 고정값으로
    // 안전하게 폴백한다.
    const measureZoomControl = () => {
      const candidates = container.querySelectorAll("img");
      for (const img of Array.from(candidates)) {
        const parent = img.parentElement;
        if (!parent) continue;
        const rect = parent.getBoundingClientRect();
        const looksLikeZoomControl =
          rect.width > 0 &&
          rect.width <= 60 &&
          rect.height >= 40 &&
          rect.height <= 150;
        if (looksLikeZoomControl) {
          const containerRect = container.getBoundingClientRect();
          onZoomControlRectRef.current({
            right: containerRect.right - rect.right,
            top: rect.top - containerRect.top,
          });
          return;
        }
      }
      onZoomControlRectRef.current(null);
    };
    // 컨트롤이 실제로 DOM에 그려질 시간을 한 프레임 준다.
    const measureFrame = window.requestAnimationFrame(measureZoomControl);
    window.addEventListener("resize", measureZoomControl);

    clustererRef.current = new kakao.maps.MarkerClusterer({
      map,
      averageCenter: true,
      minLevel: 6,
      gridSize: 80,
      styles: [createClustererStyle()],
    });

    const emitBounds = () => {
      const bounds = map.getBounds();
      const sw = bounds.getSouthWest();
      const ne = bounds.getNorthEast();
      onBoundsChangeRef.current({
        southWestLatitude: sw.getLat(),
        southWestLongitude: sw.getLng(),
        northEastLatitude: ne.getLat(),
        northEastLongitude: ne.getLng(),
      });
    };

    // 요청서 3.4 — 상세 진입 전 지도 위치를 sessionStorage에 저장한다. bounds 조회와
    // 같은 idle 이벤트를 재사용하되, 저장 자체는 디바운스 없이 즉시 한다(연산이 가볍다).
    const saveMapState = () => {
      const center = map.getCenter();
      const state: SavedMapState = {
        lat: center.getLat(),
        lng: center.getLng(),
        level: map.getLevel(),
      };
      try {
        sessionStorage.setItem(MAP_STATE_STORAGE_KEY, JSON.stringify(state));
      } catch {
        // sessionStorage를 못 쓰는 환경(프라이빗 모드 등)이어도 지도 자체는 계속 동작해야 한다.
      }
    };

    // MAP-03 규칙: idle 이벤트 후 300ms 디바운스
    kakao.maps.event.addListener(map, "idle", () => {
      saveMapState();
      if (debounceTimerRef.current !== null) {
        window.clearTimeout(debounceTimerRef.current);
      }
      debounceTimerRef.current = window.setTimeout(
        emitBounds,
        MAP_BOUNDS_DEBOUNCE_MS,
      );
    });

    // 최초 진입 시 기본 중심 기준으로 한 번 조회
    emitBounds();

    return () => {
      if (debounceTimerRef.current !== null) {
        window.clearTimeout(debounceTimerRef.current);
      }
      window.cancelAnimationFrame(measureFrame);
      window.removeEventListener("resize", measureZoomControl);
    };
  }, []);

  // 모바일 브라우저 주소창/하단바와 반응형 레이아웃 때문에 컨테이너 크기가
  // 지도 생성 뒤 바뀔 수 있다. SDK에 새 크기를 알려 타일이 빈 화면으로 남는 것을 막는다.
  // 지도 생성 이펙트와 분리해야 React 개발 모드의 이펙트 재실행 뒤에도 감시가 유지된다.
  useEffect(() => {
    const container = containerRef.current;
    if (!container) return;

    const resizeObserver = new ResizeObserver(() => {
      if (resizeFrameRef.current !== null) {
        window.cancelAnimationFrame(resizeFrameRef.current);
      }
      resizeFrameRef.current = window.requestAnimationFrame(() => {
        const map = mapRef.current;
        if (map) {
          const center = map.getCenter();
          map.relayout();
          map.setCenter(center);
        }
        resizeFrameRef.current = null;
      });
    });
    resizeObserver.observe(container);

    return () => {
      resizeObserver.disconnect();
      if (resizeFrameRef.current !== null) {
        window.cancelAnimationFrame(resizeFrameRef.current);
        resizeFrameRef.current = null;
      }
    };
  }, []);

  // 현재 위치 마커 — 최초 GPS 확보 시에만 지도 중심을 이동한다(이후 사용자가 지도를 움직여도
  // 강제로 되돌리지 않음). 복원된 지도 상태가 있었다면 hasCenteredOnGpsRef가 이미 true라서
  // 여기서는 마커 위치만 갱신하고 지도 중심은 건드리지 않는다.
  useEffect(() => {
    const map = mapRef.current;
    if (!map || !currentPosition) return;

    const position = new kakao.maps.LatLng(
      currentPosition.latitude,
      currentPosition.longitude,
    );

    if (!currentLocationMarkerRef.current) {
      currentLocationMarkerRef.current = new kakao.maps.Marker({
        position,
        image: createCurrentLocationMarkerImage(),
        zIndex: 10,
      });
      currentLocationMarkerRef.current.setMap(map);
    } else {
      currentLocationMarkerRef.current.setPosition(position);
    }

    if (!hasCenteredOnGpsRef.current) {
      map.setCenter(position);
      hasCenteredOnGpsRef.current = true;
    }
  }, [currentPosition]);

  // 요청서 4단계 — "현재 위치" 버튼. recenterSignal이 실제로 바뀌었을 때만(마운트 시
  // 최초 값과 같으면 스킵) currentPosition으로 지도 중심을 옮긴다. 스탬프 인증용 GPS
  // 정확도 판정과는 무관 — 단순히 알고 있는 현재 위치로 지도만 이동한다.
  useEffect(() => {
    if (recenterSignal === lastRecenterSignalRef.current) return;
    lastRecenterSignalRef.current = recenterSignal;

    const map = mapRef.current;
    if (!map || !currentPosition) return;
    map.setCenter(
      new kakao.maps.LatLng(currentPosition.latitude, currentPosition.longitude),
    );
  }, [recenterSignal, currentPosition]);

  // 관광지 마커 — spots나 선택 상태가 바뀔 때마다 다시 그린다
  useEffect(() => {
    const map = mapRef.current;
    const clusterer = clustererRef.current;
    if (!map || !clusterer) return;

    clusterer.clear();
    markersRef.current.forEach((marker) => marker.setMap(null));

    const images = {
      stamp: createAttractionMarkerImage("stamp"),
      "stamp-visited": createAttractionMarkerImage("stamp-visited"),
      general: createAttractionMarkerImage("general"),
      stampSelected: createAttractionMarkerImage("stamp", true),
      "stamp-visitedSelected": createAttractionMarkerImage("stamp-visited", true),
      generalSelected: createAttractionMarkerImage("general", true),
    };

    const markers = spots.map((spot) => {
      // 요청서 1.2 — 우선순위: VISITED > STAMP_TARGET/stampEnabled > GENERAL.
      // type === "STAMP_TARGET"이 실제 서버 계약(백엔드 문서 MAP-01)의 스탬프 대상
      // 판정 기준이라 그대로 쓴다 — stampEnabled를 OR로 추가하지 않는다(의미가 다르면
      // 임의로 합치지 말라는 요청서 지시).
      const variant: "stamp" | "stamp-visited" | "general" =
        spot.type === "STAMP_TARGET"
          ? spot.visitState === "VISITED"
            ? "stamp-visited"
            : "stamp"
          : "general";
      const selected = spot.id === selectedSpotId;
      const image = selected
        ? images[`${variant}Selected` as keyof typeof images]
        : images[variant];

      const marker = new kakao.maps.Marker({
        position: new kakao.maps.LatLng(
          spot.position.latitude,
          spot.position.longitude,
        ),
        image,
        title: spot.name,
        zIndex: selected ? 5 : 1,
      });

      kakao.maps.event.addListener(marker, "click", () => {
        onSelectSpotRef.current(spot.id);
      });

      return marker;
    });

    markersRef.current = markers;
    clusterer.addMarkers(markers);
  }, [spots, selectedSpotId]);

  return <div ref={containerRef} className={styles.mapContainer} />;
}
