export const ROUTES = {
  map: "/",
  collection: "/collection",
  titles: "/titles",
  favorites: "/favorites",
  attractionDetail: (id: number | string = ":attractionId") =>
    `/attractions/${id}`,
  attractionParking: (id: number | string = ":attractionId") =>
    `/attractions/${id}/parking`,
  attractionStamp: (id: number | string = ":attractionId") =>
    `/attractions/${id}/stamp`,
} as const;
