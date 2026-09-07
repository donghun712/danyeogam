import { useEffect, useRef } from "react";
import { MAP_BOUNDS_DEBOUNCE_MS } from "@/constants/api";
import {
  createAttractionMarkerImage,
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
}

const DEFAULT_CENTER = { latitude: 35.8242, longitude: 127.148 }; // 전주 한옥마을 인근 — 최초 진입 시 GPS 확보 전 기본 좌표
const DEFAULT_LEVEL = 5;

/**
 * 카카오맵 인스턴스 자체는 React 상태로 관리하지 않고 ref에 보관한다(SDK가 명령형 API이기 때문).
 * spots/currentPosition이 바뀔 때마다 마커만 다시 그린다.
 */
export function MapView({
  currentPosition,
  spots,
  onBoundsChange,
  onSelectSpot,
}: MapViewProps) {
  const containerRef = useRef<HTMLDivElement | null>(null);
  const mapRef = useRef<kakao.maps.Map | null>(null);
  const clustererRef = useRef<kakao.maps.MarkerClusterer | null>(null);
  const markersRef = useRef<kakao.maps.Marker[]>([]);
  const currentLocationMarkerRef = useRef<kakao.maps.Marker | null>(null);
  const hasCenteredOnGpsRef = useRef(false);
  const debounceTimerRef = useRef<number | null>(null);
  const resizeFrameRef = useRef<number | null>(null);
  const onBoundsChangeRef = useRef(onBoundsChange);
  const onSelectSpotRef = useRef(onSelectSpot);

  // 렌더링 중에는 ref를 쓰지 않고, 콜백이 바뀔 때만 이펙트에서 최신 값을 동기화한다.
  useEffect(() => {
    onBoundsChangeRef.current = onBoundsChange;
  }, [onBoundsChange]);
  useEffect(() => {
    onSelectSpotRef.current = onSelectSpot;
  }, [onSelectSpot]);

  // 지도 최초 생성 — 한 번만 실행
  useEffect(() => {
    if (!containerRef.current || mapRef.current) return;

    const map = new kakao.maps.Map(containerRef.current, {
      center: new kakao.maps.LatLng(
        DEFAULT_CENTER.latitude,
        DEFAULT_CENTER.longitude,
      ),
      level: DEFAULT_LEVEL,
    });
    mapRef.current = map;

    clustererRef.current = new kakao.maps.MarkerClusterer({
      map,
      averageCenter: true,
      minLevel: 6,
      gridSize: 80,
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

    // MAP-03 규칙: idle 이벤트 후 300ms 디바운스
    kakao.maps.event.addListener(map, "idle", () => {
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

  // 현재 위치 마커 — 최초 GPS 확보 시에만 지도 중심을 이동한다(이후 사용자가 지도를 움직여도 강제로 되돌리지 않음)
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

  // 관광지 마커 — spots가 바뀔 때마다 다시 그린다
  useEffect(() => {
    const map = mapRef.current;
    const clusterer = clustererRef.current;
    if (!map || !clusterer) return;

    clusterer.clear();
    markersRef.current.forEach((marker) => marker.setMap(null));

    const stampImage = createAttractionMarkerImage("stamp");
    const stampVisitedImage = createAttractionMarkerImage("stamp-visited");
    const generalImage = createAttractionMarkerImage("general");

    const markers = spots.map((spot) => {
      const image =
        spot.type === "STAMP_TARGET"
          ? spot.visitState === "VISITED"
            ? stampVisitedImage
            : stampImage
          : generalImage;

      const marker = new kakao.maps.Marker({
        position: new kakao.maps.LatLng(
          spot.position.latitude,
          spot.position.longitude,
        ),
        image,
        title: spot.name,
      });

      kakao.maps.event.addListener(marker, "click", () => {
        onSelectSpotRef.current(spot.id);
      });

      return marker;
    });

    markersRef.current = markers;
    clusterer.addMarkers(markers);
  }, [spots]);

  return <div ref={containerRef} className={styles.mapContainer} />;
}
