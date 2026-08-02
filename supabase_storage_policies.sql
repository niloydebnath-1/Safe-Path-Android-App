-- Competition prototype only.
-- Create a PUBLIC bucket named hazard-images in Supabase Storage first.
-- These policies let the Android app upload with the publishable/anon key.
-- Because Firebase Authentication is separate from Supabase Auth, this is not
-- sufficiently secure for a public production launch. Use a trusted backend or
-- signed upload URLs before production.

create policy "Prototype public read for hazard images"
on storage.objects for select
to public
using (bucket_id = 'hazard-images');

create policy "Prototype anonymous insert for hazard images"
on storage.objects for insert
to anon
with check (
  bucket_id = 'hazard-images'
  and lower(storage.extension(name)) in ('jpg', 'jpeg', 'png', 'webp')
);
