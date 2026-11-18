-- TiendaZapatosBD | Fotografias reales de productos
-- Solo cambia la columna imagen_url de los 5 modelos existentes.
-- No modifica tablas, tallas, stock, usuarios ni precios.
-- URLs de producto de terceros: requieren internet, pueden cambiar o bloquear hotlinks.
-- Para uso permanente, obtener permiso para hospedar fotos propias/autorizadas.

BEGIN;

WITH fotos(nombre, enlace) AS (
    VALUES
    ( 'Blazer Mid ''77', 'https://www.nike.ae/dw/image/v2/BDVB_PRD/on/demandware.static/-/Sites-akeneo-master-catalog/default/dw0768d748/nk/389/b/7/0/9/e/389b709e_5102_4e55_aa5d_07099b500831.jpg' ),
    ( 'Daily 4.0', 'https://assets.adidas.com/images/w_1880%2Cf_auto%2Cq_auto/67b468d7479f47d8bc06caac354167a2_9366/IF6659_01_standard.jpg' ),
    ( 'Zapatillas de tenis vintage Club C 85', 'https://reebok.ca/cdn/shop/files/100233954B3495.jpg?v=1750987507' ),
    ( 'Energen Lux', 'https://i5.walmartimages.com/seo/Reebok-Energen-Lux-Men-s-Running-Shoes_7a6919d5-5bd7-43ff-9fe0-1aaf758e5bc7.a7f6dda5dec9ff2b07097d4f679a79a4.jpeg' ),
    ( 'Nike Revolution 8.', 'https://prochampions.vtexassets.com/arquivos/ids/1208164-800-800?aspect=true&height=800&v=638811165007970000&width=800' )
)
UPDATE public.zapatos AS z
SET imagen_url = fotos.enlace
FROM fotos
WHERE z.nombre = fotos.nombre
  AND z.imagen_url IS DISTINCT FROM fotos.enlace;

-- Confirmar que ahora se registraron enlaces web para los cinco modelos.
SELECT nombre, imagen_url FROM public.zapatos ORDER BY nombre;

COMMIT;
